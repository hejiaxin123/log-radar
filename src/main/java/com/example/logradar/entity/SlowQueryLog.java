package com.example.logradar.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("slow_query_log")
public class SlowQueryLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String methodName;
    private Long executionTimeMs;
    private LocalDateTime createTime;

    public SlowQueryLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}