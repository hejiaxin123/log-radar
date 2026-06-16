package com.example.logradar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;

@Document(indexName = "log_radar")
public class LogDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Keyword)
    private String sourceIp;

    @Field(type = FieldType.Text)
    private String message;

    public LogDocument() {}

    public LogDocument(LogRecord record) {
        this.id = record.getId();
        this.timestamp = record.getTimestamp();
        this.level = record.getLevel();
        this.sourceIp = record.getSourceIp();
        this.message = record.getMessage();
    }

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
}