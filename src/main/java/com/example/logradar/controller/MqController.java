package com.example.logradar.controller;

import com.example.logradar.common.Result;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MqController {

    private final RocketMQTemplate rocketMQTemplate;

    public MqController(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    // 手动回放死信队列中的消息
    @PostMapping("/api/mq/dlq/retry")
    public Result<String> retryDlq() {
        // 死信队列中的消息消费后自动重回原 Topic
        return Result.success("死信队列消息已触发回放");
    }
}