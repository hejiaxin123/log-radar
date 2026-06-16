package com.example.logradar.server;

import com.example.logradar.service.LogService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
public class NettyServer {

    private final LogService logService;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(LogService logService) {
        this.logService = logService;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        // TCP 服务器
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap tcpBootstrap = new ServerBootstrap();
        tcpBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new SyslogHandler(logService));
                    }
                });
        tcpBootstrap.bind(5140).sync();
        System.out.println("Netty TCP Server started on port 5140");

        // UDP 服务器
        Bootstrap udpBootstrap = new Bootstrap();
        udpBootstrap.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ch.pipeline().addLast(new SyslogHandler(logService));
                    }
                });
        udpBootstrap.bind(5140).sync();
        System.out.println("Netty UDP Server started on port 5140");
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}