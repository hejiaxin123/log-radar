package com.example.logradar.server;

import com.example.logradar.entity.LogRecord;
import com.example.logradar.service.LogService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.time.LocalDateTime;

public class SyslogHandler extends SimpleChannelInboundHandler<String> {

    private final LogService logService;

    public SyslogHandler(LogService logService) {
        this.logService = logService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // 解析 Syslog 格式的日志
        LogRecord log = parseSyslog(msg);
        if (log != null) {
            logService.save(log);
        }
    }

    private LogRecord parseSyslog(String raw) {
        // 简化版 Syslog 解析：<优先级>时间戳 主机名 消息
        try {
            String[] parts = raw.split(" ", 4);
            if (parts.length < 4) return null;
            LogRecord log = new LogRecord();
            log.setTimestamp(LocalDateTime.now());
            log.setSourceIp(parts[2]); // 主机名作为来源IP
            log.setMessage(parts[3]);  // 消息内容
            log.setLevel("INFO");      // 默认级别
            return log;
        } catch (Exception e) {
            return null;
        }
    }
}