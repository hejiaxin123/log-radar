package com.example.logradar.controller;

import com.example.logradar.common.Result;
import com.example.logradar.entity.AlertRule;
import com.example.logradar.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {

    private final AlertService alertService;

    public AlertRuleController(AlertService alertService) {
        this.alertService = alertService;
    }

    // 查看所有规则
    @GetMapping
    public Result<List<AlertRule>> list() {
        return Result.success(alertService.list());
    }

    // 更新规则（阈值、冷却期、退避开关等）
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody AlertRule rule) {
        rule.setId(id);
        boolean success = alertService.updateById(rule);
        if (success) {
            // 立即刷新缓存
            alertService.reloadRules();
            return Result.success("规则已更新并生效");
        }
        return Result.error(500, "更新失败");
    }
}