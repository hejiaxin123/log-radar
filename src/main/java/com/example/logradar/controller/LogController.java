package com.example.logradar.controller;

import com.example.logradar.common.Result;
import com.example.logradar.config.RateLimiterConfig;
import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.repository.LogDocumentRepository;
import com.example.logradar.service.AlertService;
import com.example.logradar.service.LogService;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
public class LogController {

    private final LogService logService;
    private final AlertService alertService;
    private final LogDocumentRepository logDocumentRepository;
    private final RateLimiterConfig rateLimiterConfig;

    public LogController(LogService logService, AlertService alertService,
                         LogDocumentRepository logDocumentRepository,
                         RateLimiterConfig rateLimiterConfig) {
        this.logService = logService;
        this.alertService = alertService;
        this.logDocumentRepository=logDocumentRepository;
        this.rateLimiterConfig=rateLimiterConfig;
    }

    // 日志上报接口（加限流）
    @PostMapping("/api/logs")
    public Result<String> report(@RequestBody String body) {
        // 限流判断：如果拿不到令牌，返回 429 提示稍后重试
        if (!rateLimiterConfig.getRateLimiter().tryAcquire()) {
            return Result.error(429, "日志上报过于频繁，请稍后重试");
        }

        LogRecord log = logService.parseLog(body);
        if (log == null) return Result.error(400, "日志格式错误");
        logService.save(log);
        alertService.checkAlert(log.getLevel());
        return Result.success("日志上报成功");
    }
    // 动态调整 QPS 的管理接口
    @PutMapping("/api/admin/rate-limit")
    public Result<String> updateRateLimit(@RequestParam double qps) {
        rateLimiterConfig.updateRate(qps);
        return Result.success("限流阈值已更新为 " + qps + " QPS");
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

    // LogController.java 中加健康检查接口
    @GetMapping("/api/health/consistency")
    public Result<Map<String, Object>> checkConsistency() {
        Map<String, Object> result = new HashMap<>();

        // MySQL 数据量
        long mysqlCount = logService.count();
        result.put("mysqlCount", mysqlCount);

        // ES 数据量
        long esCount = logDocumentRepository.count();
        result.put("esCount", esCount);

        // 差异
        result.put("diff", mysqlCount - esCount);
        result.put("status", mysqlCount == esCount ? "一致" : "不一致");

        // Canal 最后同步时间（从 ES 中取最新一条日志的时间戳）
        LocalDateTime lastSyncTime = logService.getLastSyncTime();
        result.put("lastSyncTime", lastSyncTime);

        return Result.success(result);
    }
}