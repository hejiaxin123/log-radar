package com.example.logradar.server;

import com.example.logradar.entity.LogRecord;
import com.example.logradar.service.LogService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SyslogHandler extends SimpleChannelInboundHandler<String> {

    private final LogService logService;

    public SyslogHandler(LogService logService) {
        this.logService = logService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // 统一走责任链解析器，不再自己重复实现解析逻辑
        LogRecord log = logService.parseLog(msg);
        if (log != null) {
            logService.save(log);
        }
    }
}