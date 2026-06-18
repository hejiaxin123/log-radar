package com.example.logradar.parser;

import com.example.logradar.entity.LogRecord;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class SyslogParser implements LogParser {

    @Override
    public LogRecord parse(String raw) {
        if (!raw.startsWith("<")) return null;
        try {
            String[] parts = raw.split(" ", 4);
            if (parts.length < 4) return null;
            LogRecord log = new LogRecord();
            log.setTimestamp(LocalDateTime.now());
            log.setLevel("INFO");
            log.setSourceIp(parts[2]);
            log.setMessage(parts[3]);
            return log;
        } catch (Exception e) {
            return null;
        }
    }
}