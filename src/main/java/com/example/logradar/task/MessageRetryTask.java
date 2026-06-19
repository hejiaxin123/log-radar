package com.example.logradar.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.logradar.entity.LogMessage;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.mapper.LogMapper;
import com.example.logradar.mapper.LogMessageMapper;
import com.example.logradar.service.LogProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MessageRetryTask {

    private final LogMessageMapper logMessageMapper;
    private final LogMapper logMapper;
    private final LogProducer logProducer;

    public MessageRetryTask(LogMessageMapper logMessageMapper, LogMapper logMapper, LogProducer logProducer) {
        this.logMessageMapper = logMessageMapper;
        this.logMapper = logMapper;
        this.logProducer = logProducer;
    }

    @Scheduled(fixedDelay = 10000) // 每 10 秒扫描一次
    public void retryPendingMessages() {
        List<LogMessage> pendingList = logMessageMapper.selectList(
                new LambdaQueryWrapper<LogMessage>()
                        .eq(LogMessage::getStatus, "PENDING")
                        .lt(LogMessage::getRetryCount, 5) // 最多重试 5 次
        );

        for (LogMessage msg : pendingList) {
            try {
                LogRecord log = logMapper.selectById(msg.getLogId());
                if (log == null) {
                    msg.setStatus("FAILED");
                    logMessageMapper.updateById(msg);
                    continue;
                }
                logProducer.send("log-topic", log);
                msg.setStatus("SENT");
            } catch (Exception e) {
                msg.setRetryCount(msg.getRetryCount() + 1);
                if (msg.getRetryCount() >= 5) {
                    msg.setStatus("FAILED");
                }
            }
            logMessageMapper.updateById(msg);
        }
    }
}