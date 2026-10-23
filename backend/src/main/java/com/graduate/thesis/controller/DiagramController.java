package com.graduate.thesis.controller;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.ActivityConfig;
import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.ClassConfig;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.SequenceConfig;
import com.graduate.thesis.dto.SwimlaneConfig;
import com.graduate.thesis.dto.UseCaseConfig;
import com.graduate.thesis.service.ActivityDiagramRuleEngine;
import com.graduate.thesis.service.ArchitectureRuleEngine;
import com.graduate.thesis.service.ClassDiagramRuleEngine;
import com.graduate.thesis.service.DiagramGenerator;
import com.graduate.thesis.service.SequenceRuleEngine;
import com.graduate.thesis.service.SwimlaneRuleEngine;
import com.graduate.thesis.service.UseCaseRuleEngine;
import com.graduate.thesis.dsl.DiagramDslService;
import com.graduate.thesis.dsl.DslParseResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统设计图接口: 自动生成 / 保存 / 加载
 */
@RestController
@RequestMapping("/diagram")
public class DiagramController {

    private final DiagramGenerator diagramGenerator;
    private final ArchitectureRuleEngine ruleEngine;
    private final SwimlaneRuleEngine swimlaneEngine;
    private final UseCaseRuleEngine useCaseEngine;
    private final SequenceRuleEngine sequenceEngine;
    private final ClassDiagramRuleEngine classEngine;
    private final ActivityDiagramRuleEngine activityEngine;
    private final DiagramDslService dslService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path root;

    public DiagramController(@Value("${thesis.storage.dir}") String storageDir,
                             DiagramGenerator diagramGenerator,
                             ArchitectureRuleEngine ruleEngine,
                             SwimlaneRuleEngine swimlaneEngine,
                             UseCaseRuleEngine useCaseEngine,
                             SequenceRuleEngine sequenceEngine,
                             ClassDiagramRuleEngine classEngine,
                             ActivityDiagramRuleEngine activityEngine,
                             DiagramDslService dslService) {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize().resolve("diagrams");
        this.diagramGenerator = diagramGenerator;
        this.ruleEngine = ruleEngine;
        this.swimlaneEngine = swimlaneEngine;
        this.useCaseEngine = useCaseEngine;
        this.sequenceEngine = sequenceEngine;
        this.classEngine = classEngine;
        this.activityEngine = activityEngine;
        this.dslService = dslService;
    }

    /**
     * 自动生成设计图: ARCH/SWIMLANE/USECASE/SEQUENCE/CLASS/ACTIVITY 支持结构化配置(规则引擎), 其余支持文本描述。
     * 传入 dsl 时优先按文本解析，解析失败返回 code=400 且 data 为结构化错误(行号/片段/语法提示)。
     */
    @PostMapping("/generate")
    public Result<Object> generate(@RequestBody GenDTO dto) {
        // 文本 DSL 优先: 有文本时先解析成结构化配置, 再复用原有的规则引擎链路。
        // 解析失败直接返回结构化错误(行号/片段/语法提示), 不落到规则引擎。
        if (dto.getDsl() != null && !dto.getDsl().isBlank()) {
            DslParseResult pr = dslService.parse(dto.getType(), dto.getDsl());
            if (!pr.isOk()) {
                DslParseResult.DslError err = pr.getError();
                return Result.fail(400, err.getMessage(), err);
            }
            if (pr.getConfig() instanceof SequenceConfig) {
                dto.setSequence((SequenceConfig) pr.getConfig());
            }
        }
        if ("ARCH".equalsIgnoreCase(dto.getType()) && dto.getConfig() != null) {
            return Result.ok(ruleEngine.build(dto.getConfig()));
        }
        if ("SWIMLANE".equalsIgnoreCase(dto.getType()) && dto.getSwimlane() != null) {
            return Result.ok(swimlaneEngine.build(dto.getSwimlane()));
        }
        if ("USECASE".equalsIgnoreCase(dto.getType()) && dto.getUseCase() != null) {
            return Result.ok(useCaseEngine.build(dto.getUseCase()));
        }
        if ("SEQUENCE".equalsIgnoreCase(dto.getType()) && dto.getSequence() != null) {
            return Result.ok(sequenceEngine.build(dto.getSequence()));
        }
        if ("CLASS".equalsIgnoreCase(dto.getType()) && dto.getClassConfig() != null) {
            return Result.ok(classEngine.build(dto.getClassConfig()));
        }
        if ("ACTIVITY".equalsIgnoreCase(dto.getType()) && dto.getActivity() != null) {
            return Result.ok(activityEngine.build(dto.getActivity()));
        }
        return Result.ok(diagramGenerator.generate(dto.getType(), dto.getDescription()));
    }

    @PostMapping("/save")
    public Result<DiagramVO> save(@RequestBody DiagramVO dto) {
        Long userId = UserContext.get();
        try {
            Files.createDirectories(root);
            if (dto.getId() == null) {
                dto.setId(System.currentTimeMillis());
            }
            Path target = root.resolve(userId + "_" + dto.getId() + ".json");
            objectMapper.writeValue(target.toFile(), dto);
            return Result.ok(dto);
        } catch (Exception e) {
            throw new BusinessException(500, "保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<DiagramVO>> list() {
        Long userId = UserContext.get();
        List<DiagramVO> list = new ArrayList<>();
        try {
            Files.createDirectories(root);
            try (java.util.stream.Stream<Path> paths = Files.list(root)) {
                paths.filter(p -> p.getFileName().toString().startsWith(userId + "_"))
                        .filter(p -> p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                list.add(objectMapper.readValue(p.toFile(), DiagramVO.class));
                            } catch (Exception ignore) {
                            }
                        });
            }
            return Result.ok(list);
        } catch (Exception e) {
            throw new BusinessException(500, "列表加载失败: " + e.getMessage());
        }
    }

    @GetMapping("/load")
    public Result<DiagramVO> load(@RequestParam Long id) {
        Long userId = UserContext.get();
        Path target = root.resolve(userId + "_" + id + ".json");
        if (!Files.exists(target)) {
            throw new BusinessException(404, "设计图不存在");
        }
        try {
            return Result.ok(objectMapper.readValue(target.toFile(), DiagramVO.class));
        } catch (Exception e) {
            throw new BusinessException(500, "加载失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        Long userId = UserContext.get();
        try {
            Files.deleteIfExists(root.resolve(userId + "_" + id + ".json"));
            return Result.ok("ok");
        } catch (Exception e) {
            throw new BusinessException(500, "删除失败: " + e.getMessage());
        }
    }

    /** 生成请求 */
    public static class GenDTO {
        private String type;
        private String description;
        /**
         * 文本 DSL。非空时优先于结构化配置：先解析为对应 Config 再走规则引擎。
         * 尚未实现方言的图类型会返回“尚未开放”，此时前端应回退到表单。
         */
        private String dsl;
        private ArchitectureConfig config;
        private SwimlaneConfig swimlane;
        private UseCaseConfig useCase;
        private SequenceConfig sequence;
        private ClassConfig classConfig;
        private ActivityConfig activity;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDsl() { return dsl; }
        public void setDsl(String dsl) { this.dsl = dsl; }
        public ArchitectureConfig getConfig() { return config; }
        public void setConfig(ArchitectureConfig config) { this.config = config; }
        public SwimlaneConfig getSwimlane() { return swimlane; }
        public void setSwimlane(SwimlaneConfig swimlane) { this.swimlane = swimlane; }
        public UseCaseConfig getUseCase() { return useCase; }
        public void setUseCase(UseCaseConfig useCase) { this.useCase = useCase; }
        public SequenceConfig getSequence() { return sequence; }
        public void setSequence(SequenceConfig sequence) { this.sequence = sequence; }
        public ClassConfig getClassConfig() { return classConfig; }
        public void setClassConfig(ClassConfig classConfig) { this.classConfig = classConfig; }
        public ActivityConfig getActivity() { return activity; }
        public void setActivity(ActivityConfig activity) { this.activity = activity; }
    }
}
