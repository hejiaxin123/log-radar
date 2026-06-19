package com.example.logradar.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String level;
    private Integer windowSeconds;
    private Integer threshold;
    private Integer cooldownMinutes;
    private Integer enabled;
    private LocalDateTime createTime;
    private String notifyType;
    // 新增：是否启用指数退避（0=固定冷却，1=指数退避）
    private Integer backoffEnabled;
    // 新增：退避基数（单位：分钟，默认5）
    private Integer backoffBaseMinutes;

    public AlertRule() {}

    // getter/setter 用 IDEA 生成
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(Integer windowSeconds) { this.windowSeconds = windowSeconds; }
    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getNotifyType() { return notifyType; }
    public void setNotifyType(String notifyType) { this.notifyType = notifyType; }
    public Integer getBackoffEnabled() { return backoffEnabled; }
    public void setBackoffEnabled(Integer backoffEnabled) { this.backoffEnabled = backoffEnabled; }
    public Integer getBackoffBaseMinutes() { return backoffBaseMinutes; }
    public void setBackoffBaseMinutes(Integer backoffBaseMinutes) { this.backoffBaseMinutes = backoffBaseMinutes; }
}