// entity/LogMessage.java
package com.example.logradar.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("log_message")
public class LogMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long logId;
    private String status;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public LogMessage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}