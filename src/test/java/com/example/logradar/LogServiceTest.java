package com.example.logradar;

import com.example.logradar.entity.AlertRule;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.service.AlertService;
import com.example.logradar.service.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LogServiceTest {

    @Autowired
    private LogService logService;
    @Autowired
    private AlertService alertService;

    @Test
    public void testSave() {
        LogRecord log = new LogRecord();
        log.setTimestamp(LocalDateTime.now());
        log.setLevel("TEST");
        log.setSourceIp("127.0.0.1");
        log.setMessage("JUnit 测试日志");

        boolean result = logService.save(log);
        assertTrue(result);
        assertNotNull(log.getId());
        System.out.println("测试通过：日志 ID = " + log.getId());
    }

    @Test
    public void testParseLog() {
        String rawLog = "[2026-06-15 10:00:00] [ERROR] [192.168.1.1] Connection timeout";
        LogRecord log = logService.parseLog(rawLog);
        assertNotNull(log);
        assertEquals("ERROR", log.getLevel());
        assertEquals("192.168.1.1", log.getSourceIp());
        assertEquals("Connection timeout", log.getMessage());
        System.out.println("测试通过：日志解析成功，级别 = " + log.getLevel());
    }

    @Test
    public void testReloadRules() {
        alertService.reloadRules();
        AlertRule rule = alertService.getRulesCache().stream()
                .filter(r -> r.getLevel().equals("ERROR"))
                .findFirst()
                .orElse(null);
        assertNotNull(rule);
        assertEquals("ERROR告警", rule.getName());
        System.out.println("测试通过：告警规则加载成功，规则名称 = " + rule.getName());
    }
}