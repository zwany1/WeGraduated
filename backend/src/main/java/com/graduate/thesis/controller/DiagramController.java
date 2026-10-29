package com.graduate.thesis.controller;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.ActivityConfig;
import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.ClassConfig;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.ErDTO;
import com.graduate.thesis.dto.FlowConfig;
import com.graduate.thesis.dto.SequenceConfig;
import com.graduate.thesis.dto.SwimlaneConfig;
import com.graduate.thesis.dto.UseCaseConfig;
import com.graduate.thesis.service.ActivityDiagramRuleEngine;
import com.graduate.thesis.service.ArchitectureRuleEngine;
import com.graduate.thesis.service.ClassDiagramRuleEngine;
import com.graduate.thesis.service.DiagramGenerator;
import com.graduate.thesis.service.ERGraphService;
import com.graduate.thesis.service.FlowRuleEngine;
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
    private final FlowRuleEngine flowEngine;
    private final ERGraphService erGraphService;
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
                             FlowRuleEngine flowEngine,
                             ERGraphService erGraphService,
                             DiagramDslService dslService) {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize().resolve("diagrams");
        this.diagramGenerator = diagramGenerator;
        this.ruleEngine = ruleEngine;
        this.swimlaneEngine = swimlaneEngine;
        this.useCaseEngine = useCaseEngine;
        this.sequenceEngine = sequenceEngine;
        this.classEngine = classEngine;
        this.activityEngine = activityEngine;
        this.flowEngine = flowEngine;
        this.erGraphService = erGraphService;
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
            } else if (pr.getConfig() instanceof FlowConfig) {
                dto.setFlow((FlowConfig) pr.getConfig());
            } else if (pr.getConfig() instanceof ArchitectureConfig) {
                dto.setConfig((ArchitectureConfig) pr.getConfig());
            } else if (pr.getConfig() instanceof ClassConfig) {
                dto.setClassConfig((ClassConfig) pr.getConfig());
            } else if (pr.getConfig() instanceof UseCaseConfig) {
                dto.setUseCase((UseCaseConfig) pr.getConfig());
            } else if (pr.getConfig() instanceof SwimlaneConfig) {
                dto.setSwimlane((SwimlaneConfig) pr.getConfig());
            } else if (pr.getConfig() instanceof ActivityConfig) {
                dto.setActivity((ActivityConfig) pr.getConfig());
            } else if (pr.getConfig() instanceof ErDTO) {
                dto.setEr((ErDTO) pr.getConfig());
            }
        }
        if ("FLOW".equalsIgnoreCase(dto.getType()) && dto.getFlow() != null) {
            Object result = flowEngine.build(dto.getFlow());
            attachDslInfo(result, dto, dto.getFlow());
            return Result.ok(result);
        }
        if ("ARCH".equalsIgnoreCase(dto.getType()) && dto.getConfig() != null) {
            Object result = ruleEngine.build(dto.getConfig());
            attachDslInfo(result, dto, dto.getConfig());
            return Result.ok(result);
        }
        if ("SWIMLANE".equalsIgnoreCase(dto.getType()) && dto.getSwimlane() != null) {
            Object result = swimlaneEngine.build(dto.getSwimlane());
            attachDslInfo(result, dto, dto.getSwimlane());
            return Result.ok(result);
        }
        if ("USECASE".equalsIgnoreCase(dto.getType()) && dto.getUseCase() != null) {
            Object result = useCaseEngine.build(dto.getUseCase());
            attachDslInfo(result, dto, dto.getUseCase());
            return Result.ok(result);
        }
        if ("SEQUENCE".equalsIgnoreCase(dto.getType()) && dto.getSequence() != null) {
            Object result = sequenceEngine.build(dto.getSequence());
            attachDslInfo(result, dto, dto.getSequence());
            return Result.ok(result);
        }
        if ("CLASS".equalsIgnoreCase(dto.getType()) && dto.getClassConfig() != null) {
            Object result = classEngine.build(dto.getClassConfig());
            attachDslInfo(result, dto, dto.getClassConfig());
            return Result.ok(result);
        }
        if ("ACTIVITY".equalsIgnoreCase(dto.getType()) && dto.getActivity() != null) {
            Object result = activityEngine.build(dto.getActivity());
            attachDslInfo(result, dto, dto.getActivity());
            return Result.ok(result);
        }
        if ("ER".equalsIgnoreCase(dto.getType()) && dto.getEr() != null) {
            // ER 走独立的图服务(Chen 记法: 实体矩形/属性椭圆/关系菱形), 与其它图不同的渲染模型
            return Result.ok(erGraphService.build(dto.getEr()));
        }
        return Result.ok(diagramGenerator.generate(dto.getType(), dto.getDescription()));
    }

    /**
     * 文本生成成功时，把 DSL 原文与解析后的配置随结果带回，前端据此回填文本视图与表单。
     * 非文本路径(纯表单/自然语言)不写，避免把表单状态覆盖成"看起来来自文本"。
     */
    private void attachDslInfo(Object result, GenDTO dto, Object config) {
        if (!(result instanceof DiagramVO)) {
            return;
        }
        if (dto.getDsl() == null || dto.getDsl().isBlank()) {
            return;
        }
        DiagramVO vo = (DiagramVO) result;
        vo.setDsl(dto.getDsl());
        vo.setConfig(config);
    }

    /**
     * 结构化配置 -> 文本 DSL(反向生成)。
     * 画布上改名/改文案后, 前端把改过的配置发过来换成文本, 保证文本视图与画布不脱节(R4)。
     */
    @PostMapping("/dsl/render")
    public Result<Object> renderDsl(@RequestBody RenderDslDTO dto) {
        String type = dto.getType() == null ? "" : dto.getType().trim();
        Object config = dto.getConfig();
        if ("SEQUENCE".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少时序图配置");
            }
            config = objectMapper.convertValue(config, SequenceConfig.class);
        } else if ("FLOW".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少流程图配置");
            }
            config = objectMapper.convertValue(config, FlowConfig.class);
        } else if ("ARCH".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少架构图配置");
            }
            config = objectMapper.convertValue(config, ArchitectureConfig.class);
        } else if ("CLASS".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少类图配置");
            }
            config = objectMapper.convertValue(config, ClassConfig.class);
        } else if ("USECASE".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少用例图配置");
            }
            config = objectMapper.convertValue(config, UseCaseConfig.class);
        } else if ("SWIMLANE".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少泳道图配置");
            }
            config = objectMapper.convertValue(config, SwimlaneConfig.class);
        } else if ("ACTIVITY".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少活动图配置");
            }
            config = objectMapper.convertValue(config, ActivityConfig.class);
        } else if ("ER".equalsIgnoreCase(type)) {
            if (config == null) {
                throw new BusinessException(400, "缺少 ER 图配置");
            }
            config = objectMapper.convertValue(config, ErDTO.class);
        }
        try {
            String text = dslService.render(type, config);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("dsl", text);
            return Result.ok(data);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    public Result<DiagramVO> save(@RequestBody DiagramVO dto) {        Long userId = UserContext.get();
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
        private FlowConfig flow;
        private ErDTO er;
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
        public FlowConfig getFlow() { return flow; }
        public void setFlow(FlowConfig flow) { this.flow = flow; }
        public ErDTO getEr() { return er; }
        public void setEr(ErDTO er) { this.er = er; }
        public ClassConfig getClassConfig() { return classConfig; }
        public void setClassConfig(ClassConfig classConfig) { this.classConfig = classConfig; }
        public ActivityConfig getActivity() { return activity; }
        public void setActivity(ActivityConfig activity) { this.activity = activity; }
    }

    /** 反向生成请求: 结构化配置 -> 文本 DSL。config 用 Object 接收, 按 type 再转成具体 Config。 */
    public static class RenderDslDTO {
        private String type;
        private Object config;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getConfig() { return config; }
        public void setConfig(Object config) { this.config = config; }
    }
}
