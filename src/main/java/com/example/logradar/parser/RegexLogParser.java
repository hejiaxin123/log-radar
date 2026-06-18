package com.example.logradar.parser;

import com.example.logradar.entity.LogRecord;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexLogParser implements LogParser {

    private static final String REGEX = "\\[(.*?)\\]\\s*\\[(.*?)\\]\\s*\\[(.*?)\\]\\s*(.*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    @Override
    public LogRecord parse(String raw) {
        Matcher matcher = PATTERN.matcher(raw);
        if (matcher.find()) {
            LogRecord log = new LogRecord();
            log.setTimestamp(LocalDateTime.parse(matcher.group(1).replace(" ", "T")));
            log.setLevel(matcher.group(2));
            log.setSourceIp(matcher.group(3));
            log.setMessage(matcher.group(4));
            return log;
        }
        return null;
    }
}