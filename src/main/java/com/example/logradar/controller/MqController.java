package com.example.logradar.controller;

import com.example.logradar.common.Result;
import com.example.logradar.service.MqService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MqController {

    private final RocketMQTemplate rocketMQTemplate;
    private final MqService mqService;

    public MqController(RocketMQTemplate rocketMQTemplate, MqService mqService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.mqService = mqService;
    }
    // 手动回放死信队列中的消息
    @PostMapping("/api/mq/dlq/retry")
    public Result<String> retryDlq() throws Exception {
        int count = mqService.retryDlqMessages();
        return Result.success("死信队列消息已触发回放，共回放 " + count + " 条");
    }
}