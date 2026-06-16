package com.example.logradar;

import com.example.logradar.entity.LogRecord;
import com.example.logradar.service.LogProducer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
public class LogRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogRadarApplication.class, args);
    }

    @Bean
    public ApplicationRunner testRocketMQ(LogProducer logProducer) {
        return args -> {
            LogRecord testLog = new LogRecord();
            testLog.setTimestamp(LocalDateTime.now());
            testLog.setLevel("TEST");
            testLog.setSourceIp("127.0.0.1");
            testLog.setMessage("RocketMQ 连接测试");
            logProducer.send("log-topic", testLog);
            System.out.println("RocketMQ 发送成功");
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
