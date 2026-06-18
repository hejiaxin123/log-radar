package com.example.logradar.parser;

import com.example.logradar.entity.LogRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public LogRecord parse(String raw) {
        try {
            return objectMapper.readValue(raw, LogRecord.class);
        } catch (Exception e) {
            return null;
        }
    }
}