package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.dto.SqlParseDTO;
import com.graduate.thesis.dto.SqlTableInfo;
import com.graduate.thesis.dto.Table3DTO;
import com.graduate.thesis.service.SqlTableParser;
import com.graduate.thesis.service.Table3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URLEncoder;

/**
 * 三线表接口: 生成三线表 docx / 解析建表 SQL
 */
@RestController
@RequestMapping("/table3")
public class Table3Controller {

    private final Table3Service table3Service;

    public Table3Controller(Table3Service table3Service) {
        this.table3Service = table3Service;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody Table3DTO dto) {
        byte[] bytes = table3Service.generate(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encode("三线表.docx"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/parse-sql")
    public Result<SqlTableInfo> parseSql(@Valid @RequestBody SqlParseDTO dto) {
        SqlTableInfo info = SqlTableParser.parse(dto.getSql());
        if (info.getColumns().isEmpty()) {
            return Result.fail(400, "未能从 SQL 中解析出列信息，请检查语句格式");
        }
        return Result.ok(info);
    }

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return name;
        }
    }
}
