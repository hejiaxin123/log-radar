package com.example.logradar.parser;

import com.example.logradar.entity.LogRecord;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SyslogParser implements LogParser {

    // RFC 3164 时间格式：Jun 19 10:30:45
    private static final DateTimeFormatter RFC3164_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd HH:mm:ss").withLocale(java.util.Locale.ENGLISH);

    // RFC 5424 时间格式：2026-06-19T10:30:45.000Z
    private static final DateTimeFormatter RFC5424_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public LogRecord parse(String raw) {
        // 必须包含 `<` 开头，否则不是 Syslog 格式，快速失败
        if (!raw.startsWith("<")) return null;

        try {
            // 提取优先级（尖括号里的数字）
            int endBracket = raw.indexOf(">");
            if (endBracket == -1) return null;
            int priority = Integer.parseInt(raw.substring(1, endBracket));
            String rest = raw.substring(endBracket + 1).trim();

            // 根据优先级推算日志级别
            String level = priorityToLevel(priority % 8);

            // 判断是 RFC 5424 还是 RFC 3164
            if (rest.startsWith("1 ")) {
                // RFC 5424: <优先级>版本 时间戳 主机名 应用名 进程ID 消息ID 结构化数据 消息
                return parseRFC5424(rest, level);
            } else {
                // RFC 3164: 时间戳 主机名 标签[进程ID]: 消息
                return parseRFC3164(rest, level);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 RFC 3164 格式：时间戳 主机名 标签[进程ID]: 消息
     */
    private LogRecord parseRFC3164(String rest, String level) {
        // 按空格分割，但消息体可能包含空格，所以用 limit
        String[] parts = rest.split(" ", 4); // 时间戳 主机名 标签 消息
        if (parts.length < 4) return null;

        LogRecord log = new LogRecord();

        // 解析时间戳（带年份补全）
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime ts = LocalDateTime.parse(parts[0] + " " + parts[1], RFC3164_FORMATTER);
            // 补全年份（RFC 3164 不带年份）
            log.setTimestamp(ts.withYear(now.getYear()));
        } catch (Exception e) {
            log.setTimestamp(LocalDateTime.now());
        }

        log.setSourceIp(parts[2]); // 主机名

        // 标签部分可能包含 [进程ID]，提取应用名
        String tag = parts[2];
        if (tag.contains("[")) {
            tag = tag.substring(0, tag.indexOf("["));
        }

        log.setMessage(parts[3]); // 消息体
        log.setLevel(level);

        return log;
    }

    /**
     * 解析 RFC 5424 格式：版本 时间戳 主机名 应用名 进程ID 消息ID 结构化数据 消息
     */
    private LogRecord parseRFC5424(String rest, String level) {
        // 跳过版本号 "1 "
        rest = rest.substring(2).trim();

        // 按空格分割：时间戳 主机名 应用名 进程ID 消息ID 结构化数据 消息
        String[] parts = rest.split(" ", 7);
        if (parts.length < 7) return null;

        LogRecord log = new LogRecord();

        // 解析时间戳
        try {
            log.setTimestamp(LocalDateTime.parse(parts[0], RFC5424_FORMATTER));
        } catch (Exception e) {
            log.setTimestamp(LocalDateTime.now());
        }

        log.setSourceIp(parts[1]); // 主机名
        log.setMessage(parts[6]);  // 消息体（第7段才是真正的消息内容）
        log.setLevel(level);

        return log;
    }

    /**
     * 根据 Syslog 优先级推算日志级别
     * 0=EMERG, 1=ALERT, 2=CRIT, 3=ERROR, 4=WARN, 5=NOTICE, 6=INFO, 7=DEBUG
     */
    private String priorityToLevel(int severity) {
        switch (severity) {
            case 0: return "FATAL";
            case 1: return "ALERT";
            case 2: return "CRITICAL";
            case 3: return "ERROR";
            case 4: return "WARN";
            case 5: return "NOTICE";
            case 6: return "INFO";
            case 7: return "DEBUG";
            default: return "INFO";
        }
    }
}