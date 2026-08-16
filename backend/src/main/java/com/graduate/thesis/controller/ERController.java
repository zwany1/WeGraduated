package com.graduate.thesis.controller;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.ERGraphVO;
import com.graduate.thesis.dto.ErDTO;
import com.graduate.thesis.dto.NodePositionDTO;
import com.graduate.thesis.service.ERDiagramRenderer;
import com.graduate.thesis.service.ERGraphService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ER 图接口: 生成 ER 图 PNG / 返回图数据结构 / 布局保存与加载
 */
@RestController
@RequestMapping("/er")
public class ERController {

    private final ERGraphService erGraphService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path layoutRoot;

    public ERController(ERGraphService erGraphService,
                        @Value("${thesis.storage.dir}") String storageDir) {
        this.erGraphService = erGraphService;
        this.layoutRoot = Paths.get(storageDir).toAbsolutePath().normalize().resolve("erlayout");
    }

    /** 解析建表 SQL 为 ER 实体与关系(支持多表与外键) */
    @PostMapping("/parse-sql")
    public Result<com.graduate.thesis.dto.ErParseResult> parseSql(@RequestBody java.util.Map<String, Object> body) {
        String sql = body.get("sql") == null ? "" : String.valueOf(body.get("sql"));
        com.graduate.thesis.dto.ErParseResult result =
                com.graduate.thesis.service.SqlTableParser.parseEr(sql);
        if (result.getEntities().isEmpty()) {
            return Result.fail(400, "未能从 SQL 中解析出表结构，请检查语句格式");
        }
        return Result.ok(result);
    }

    @PostMapping("/render")
    public ResponseEntity<byte[]> render(@Valid @RequestBody ErDTO dto) {
        if (dto.getEntities() == null || dto.getEntities().isEmpty()) {
            throw new BusinessException(400, "请至少添加一个实体");
        }
        byte[] png = ERDiagramRenderer.render(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encode("ER图.png"))
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PostMapping("/graph")
    public Result<ERGraphVO> graph(@Valid @RequestBody ErDTO dto) {
        return Result.ok(erGraphService.build(dto));
    }

    /**
     * 保存拖拽后的布局坐标
     */
    @PostMapping("/save-layout")
    public Result<String> saveLayout(@RequestBody List<NodePositionDTO> positions) {
        if (positions == null) {
            positions = new ArrayList<>();
        }
        Long userId = UserContext.get();
        try {
            Files.createDirectories(layoutRoot);
            File target = layoutRoot.resolve(userId + ".json").toFile();
            objectMapper.writeValue(target, positions);
            return Result.ok("ok");
        } catch (Exception e) {
            throw new BusinessException(500, "布局保存失败: " + e.getMessage());
        }
    }

    /**
     * 加载已保存的布局坐标
     */
    @GetMapping("/load-layout")
    public Result<List<NodePositionDTO>> loadLayout() {
        Long userId = UserContext.get();
        File target = layoutRoot.resolve(userId + ".json").toFile();
        if (!target.exists()) {
            return Result.ok(new ArrayList<>());
        }
        try {
            List<NodePositionDTO> list = objectMapper.readValue(target, new TypeReference<List<NodePositionDTO>>() {
            });
            return Result.ok(list);
        } catch (Exception e) {
            throw new BusinessException(500, "布局加载失败: " + e.getMessage());
        }
    }

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return name;
        }
    }
}
