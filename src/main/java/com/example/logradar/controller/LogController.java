package com.example.logradar.controller;

import com.example.logradar.common.Result;
import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.service.AlertService;
import com.example.logradar.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@Validated
public class LogController {
    private final LogService logService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;
    public LogController(LogService logService, AlertService alertService,ObjectMapper objectMapper) {
        this.logService = logService;
        this.alertService = alertService;
        this.objectMapper=objectMapper;
    }

    // 日志上报接口
    @PostMapping("/api/logs")
    public Result<String> report(@RequestBody String body) {
        // 自动适配：Syslog 格式以 "<" 开头
        if (body.startsWith("<")) {
            LogRecord log = logService.parseSyslog(body);
            if (log != null) {
                logService.save(log);
                return Result.success("Syslog 日志已接收");
            }
            return Result.error(400, "Syslog 格式错误");
        }
        // JSON 格式：手动反序列化
        try {
            LogRecord log = objectMapper.readValue(body, LogRecord.class);
            logService.save(log);
            return Result.success("日志上报成功");
        } catch (Exception e) {
            return Result.error(400, "JSON 格式错误");
        }
    }
    //日志检索接口
    @GetMapping("/api/logs/search")
    public Result<List<LogDocument>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return Result.success(logService.search(keyword, level, startTime, endTime));
    }
    //日志解析接口
    @PostMapping("/api/logs/parse")
    public Result<String> parse(@RequestBody String rawLog) {
        LogRecord log = logService.parseLog(rawLog);
        if (log == null) return Result.error(400, "日志格式错误");
        logService.save(log);
        alertService.checkAlert(log.getLevel());  // 上报后触发告警检查
        return Result.success("日志解析成功");
    }
    // MySQL聚合分析
    @GetMapping("/api/logs/aggregate")
    public Result<Map<String, Long>> aggregate() {
        return Result.success(logService.aggregateByLevel());
    }
    //直接用 HTTP 调用 ES 的聚合 API
    @GetMapping("/api/logs/aggregate/es")
    public Result<Map<String, Long>> aggregateES() {
        return Result.success(logService.aggregateByLevelES());
    }
}