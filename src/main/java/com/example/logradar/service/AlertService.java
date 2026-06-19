package com.example.logradar.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.logradar.entity.AlertRule;
import com.example.logradar.mapper.AlertRuleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AlertService extends ServiceImpl<AlertRuleMapper, AlertRule> {
    private final StringRedisTemplate redisTemplate;
    public AlertService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private volatile List<AlertRule> rulesCache = new ArrayList<>();

    @Scheduled(fixedDelay = 60000)
    public void reloadRules() {
        rulesCache = lambdaQuery().eq(AlertRule::getEnabled, 1).list();
    }

    // 滑动窗口检查是否触发告警
    @Async
    public void checkAlert(String level) {
        // 查出启用的规则
        // checkAlert 方法中使用 rulesCache 而不是每次查数据库
        AlertRule rule = rulesCache.stream()
                .filter(r -> r.getLevel().equals(level))
                .findFirst()
                .orElse(null);
        if (rule == null) return;

        String zsetKey = "alert:zset:" + level;
        String cooldownKey = "alert:cooldown:" + rule.getId();
        long now = System.currentTimeMillis();

        // 1. 冷却期内不重复告警
        if (redisTemplate.hasKey(cooldownKey)) return;

        // 2. 将当前日志加入 ZSET（score = 当前时间戳）
        redisTemplate.opsForZSet().add(zsetKey, String.valueOf(now), now);

        // 3. 删除窗口外的过期数据（只保留最近 windowSeconds 秒）
        long windowStart = now - rule.getWindowSeconds() * 1000L;
        redisTemplate.opsForZSet().removeRangeByScore(zsetKey, 0, windowStart);

        // 4. 统计窗口内的日志数量
        Long count = redisTemplate.opsForZSet().count(zsetKey, windowStart, now);
        if (count == null || count < rule.getThreshold()) return;

        // 5. 触发告警
        triggerAlert(rule, count);

        // 6. 计算冷却时间
        long cooldownMinutes;
        if (rule.getBackoffEnabled() != null && rule.getBackoffEnabled() == 1) {
            // 指数退避：读取当前告警次数，计算退避时间
            String backoffCountKey = "alert:backoff:count:" + rule.getId();
            Long backoffCount = redisTemplate.opsForValue().increment(backoffCountKey);
            // 第一次 count=1 → 5分钟，第二次 count=2 → 20分钟，第三次 count=3 → 45分钟...
            // 公式：base * 2^(count-1)，上限 1440 分钟（24小时）
            long base = rule.getBackoffBaseMinutes() != null ? rule.getBackoffBaseMinutes() : 5;
            cooldownMinutes = base * (1L << Math.min(backoffCount - 1, 8)); // 最多移8位，避免溢出
            cooldownMinutes = Math.min(cooldownMinutes, 1440); // 上限 24 小时
            // 设置退避计数器过期时间（问题解决后自动重置）
            redisTemplate.expire(backoffCountKey, 24, TimeUnit.HOURS);
        } else {
            // 固定冷却期
            cooldownMinutes = rule.getCooldownMinutes();
        }
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownMinutes, TimeUnit.MINUTES);

        // 7. 清理本次告警的 ZSET 数据
        redisTemplate.delete(zsetKey);
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
        // 模拟钉钉通知
        System.out.println("钉钉通知：" + msg);
    }

    private void sendEmail(String msg) {
        // 模拟邮件通知
        System.out.println("邮件通知：" + msg);
    }
    //供测试调用
    public List<AlertRule> getRulesCache() {
        return rulesCache;
    }
}