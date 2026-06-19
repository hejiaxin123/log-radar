package com.example.logradar.service;

import com.example.logradar.entity.LogRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MqService {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public MqService(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public int retryDlqMessages() throws Exception {
        DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("log-radar-dlq-retry-group");
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.subscribe("log-topic-dlq", "*");
        consumer.start();

        List<LogRecord> deadMessages = new ArrayList<>();
        // 循环拉取，每次拉 10 条，直到拉空为止
        while (true) {
            List<MessageExt> msgs = consumer.poll(3000); // 超时 3 秒
            if (msgs.isEmpty()) {
                break; // 没消息了，退出
            }
            for (MessageExt msg : msgs) {
                String body = new String(msg.getBody());
                try {
                    LogRecord log = objectMapper.readValue(body, LogRecord.class);
                    deadMessages.add(log);
                    // 回放到原 topic
                    rocketMQTemplate.convertAndSend("log-topic", log);
                    System.out.println("死信消息已回放: " + log.getId());
                } catch (Exception e) {
                    System.err.println("死信消息回放失败: " + e.getMessage());
                }
            }
        }
        consumer.shutdown();
        return deadMessages.size();
    }
}