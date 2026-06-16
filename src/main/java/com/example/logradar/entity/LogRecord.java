package com.example.logradar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("log_record")
public class LogRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime timestamp;
    private String level;
    private String sourceIp;
    private String message;
    private String parsedFields;
    private LocalDateTime createTime;

    public LogRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getParsedFields() { return parsedFields; }
    public void setParsedFields(String parsedFields) { this.parsedFields = parsedFields; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}