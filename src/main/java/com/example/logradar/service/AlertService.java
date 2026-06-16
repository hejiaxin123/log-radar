package com.example.logradar.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.logradar.entity.AlertRule;
import com.example.logradar.mapper.AlertRuleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class AlertService extends ServiceImpl<AlertRuleMapper, AlertRule> {
    private final StringRedisTemplate redisTemplate;
    public AlertService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 滑动窗口检查是否触发告警
    public void checkAlert(String level) {
        // 查出启用的规则
        AlertRule rule = lambdaQuery().eq(AlertRule::getLevel, level)
                .eq(AlertRule::getEnabled, 1)
                .one();
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

        // 6. 设置冷却期（退避）
        redisTemplate.opsForValue().set(cooldownKey, "1", rule.getCooldownMinutes(), TimeUnit.MINUTES);

        // 7. 清理本次告警的 ZSET 数据
        redisTemplate.delete(zsetKey);
    }

    // 实际告警（模拟发送通知）
    private void triggerAlert(AlertRule rule, Long count) {
        System.out.println("告警触发！规则：" + rule.getName() +
                "，级别：" + rule.getLevel() +
                "，窗口内出现次数：" + count);
    }
}