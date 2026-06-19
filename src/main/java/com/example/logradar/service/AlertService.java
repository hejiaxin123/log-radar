package com.example.logradar.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.logradar.entity.AlertRule;
import com.example.logradar.mapper.AlertRuleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AlertService extends ServiceImpl<AlertRuleMapper, AlertRule> {
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;

    // 从配置文件读取
    @org.springframework.beans.factory.annotation.Value("${alert.dingding.webhook-url:}")
    private String dingdingWebhookUrl;
    @org.springframework.beans.factory.annotation.Value("${alert.email.recipient:}")
    private String mailRecipient;
    @org.springframework.beans.factory.annotation.Value("${MAIL_USERNAME:}")
    private String mailUsername;
    @org.springframework.beans.factory.annotation.Value("${MAIL_PASSWORD:}")
    private String mailPassword;

    public AlertService(StringRedisTemplate redisTemplate,RestTemplate restTemplate,
                        JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.restTemplate=restTemplate;
        this.mailSender=mailSender;
    }

    private volatile List<AlertRule> rulesCache = new ArrayList<>();

    @Scheduled(fixedDelay = 60000)
    public void reloadRules() {
        rulesCache = lambdaQuery().eq(AlertRule::getEnabled, 1).list();
    }

    // 滑动窗口检查是否触发告警
    @Async
    public void checkAlert(String level) {
        // 查出该级别的所有启用规则（可能有多条）
        List<AlertRule> matchedRules = rulesCache.stream()
                .filter(r -> r.getLevel().equals(level))
                .toList();

        if (matchedRules.isEmpty()) return;

        // 逐条检查
        for (AlertRule rule : matchedRules) {
            String zsetKey = "alert:zset:" + rule.getId();  // 用规则ID区分，不是level
            String cooldownKey = "alert:cooldown:" + rule.getId();
            long now = System.currentTimeMillis();

            // 1. 冷却期内不重复告警
            if (redisTemplate.hasKey(cooldownKey)) continue;

            // 2. 将当前日志加入该规则的 ZSET
            redisTemplate.opsForZSet().add(zsetKey, String.valueOf(now), now);

            // 3. 删除窗口外过期数据
            long windowStart = now - rule.getWindowSeconds() * 1000L;
            redisTemplate.opsForZSet().removeRangeByScore(zsetKey, 0, windowStart);

            // 4. 统计窗口内数量
            Long count = redisTemplate.opsForZSet().count(zsetKey, windowStart, now);
            if (count == null || count < rule.getThreshold()) continue;

            // 5. 触发告警
            triggerAlert(rule, count);

            // 6. 计算冷却时间（指数退避或固定）
            long cooldownMinutes;
            if (rule.getBackoffEnabled() != null && rule.getBackoffEnabled() == 1) {
                String backoffCountKey = "alert:backoff:count:" + rule.getId();
                Long backoffCount = redisTemplate.opsForValue().increment(backoffCountKey);
                long base = rule.getBackoffBaseMinutes() != null ? rule.getBackoffBaseMinutes() : 5;
                cooldownMinutes = base * (1L << Math.min(backoffCount - 1, 8));
                cooldownMinutes = Math.min(cooldownMinutes, 1440);
                redisTemplate.expire(backoffCountKey, 24, TimeUnit.HOURS);
            } else {
                cooldownMinutes = rule.getCooldownMinutes();
            }
            redisTemplate.opsForValue().set(cooldownKey, "1", cooldownMinutes, TimeUnit.MINUTES);

            // 7. 清理本规则的 ZSET
            redisTemplate.delete(zsetKey);
        }
    }

    // 实际告警（模拟发送通知）
    private void triggerAlert(AlertRule rule, Long count) {
        String msg = "告警触发！规则：" + rule.getName() +
                "，级别：" + rule.getLevel() +
                "，窗口内出现次数：" + count;

        switch (rule.getNotifyType()) {
            case "dingding":
                sendDingding(msg);
                break;
            case "email":
                sendEmail(msg);
                break;
            default:
                System.out.println(msg); // log_only
        }
    }

    private void sendDingding(String msg) {
        try {
            System.out.println("钉钉 Webhook URL = " + dingdingWebhookUrl); // ⬅️ 加这行
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            Map<String, String> content = new HashMap<>();
            content.put("content", "【LogRadar告警】\n" + msg);
            body.put("text", content);
            restTemplate.postForEntity(dingdingWebhookUrl, body, String.class);
            System.out.println("钉钉通知已发送：" + msg);
        } catch (Exception e) {
            System.err.println("钉钉通知发送失败：" + e.getMessage());
        }
    }
    private void sendEmail(String msg) {
        if (mailUsername.isEmpty() || mailPassword.isEmpty()) {
            System.out.println("邮件未配置，跳过发送：" + msg);
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailUsername);
            mail.setTo(mailRecipient);
            mail.setSubject("【LogRadar告警通知】");
            mail.setText(msg);
            mailSender.send(mail);
            System.out.println("邮件通知已发送：" + msg);
        } catch (Exception e) {
            System.err.println("邮件通知发送失败：" + e.getMessage());
        }
    }
    //供测试调用
    public List<AlertRule> getRulesCache() {
        return rulesCache;
    }
}