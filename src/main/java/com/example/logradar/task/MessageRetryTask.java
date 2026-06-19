package com.example.logradar.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.logradar.entity.LogMessage;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.mapper.LogMapper;
import com.example.logradar.mapper.LogMessageMapper;
import com.example.logradar.service.LogProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
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

    // 每 10 秒扫描补偿
    @Scheduled(fixedDelay = 10000)
    public void retryPendingMessages() {
        List<LogMessage> pendingList = logMessageMapper.selectList(
                new LambdaQueryWrapper<LogMessage>()
                        .eq(LogMessage::getStatus, "PENDING")
                        .lt(LogMessage::getRetryCount, 5)
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

    // 新增：每天凌晨 3 点清理 7 天前的 SENT 消息
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanSentMessages() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<LogMessage> wrapper = new LambdaQueryWrapper<LogMessage>()
                .eq(LogMessage::getStatus, "SENT")
                .lt(LogMessage::getCreateTime, sevenDaysAgo);
        logMessageMapper.delete(wrapper);
        System.out.println("清理 SENT 消息完成，时间点：" + sevenDaysAgo);
    }
}