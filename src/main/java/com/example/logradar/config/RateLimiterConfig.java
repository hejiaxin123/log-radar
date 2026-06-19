package com.example.logradar.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterConfig {

    // 默认每秒 500 个请求
    private volatile RateLimiter rateLimiter = RateLimiter.create(500);

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    // 动态调整 QPS（通过接口或定时任务调用）
    public void updateRate(double newQps) {
        this.rateLimiter = RateLimiter.create(newQps);
        System.out.println("限流阈值已更新为：" + newQps + " QPS");
    }
}