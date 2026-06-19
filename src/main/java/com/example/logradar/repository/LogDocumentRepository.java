package com.example.logradar.repository;

import com.example.logradar.entity.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogDocumentRepository extends ElasticsearchRepository<LogDocument, Long> {
    List<LogDocument> findByLevel(String level);
    List<LogDocument> findByMessageContaining(String keyword);
    List<LogDocument> findByLevelAndTimestampBetween(String level, LocalDateTime startTime, LocalDateTime endTime);
    List<LogDocument> findByMessageContainingAndTimestampBetween(String keyword, LocalDateTime startTime, LocalDateTime endTime);

    // 新增：keyword + level 组合
    List<LogDocument> findByLevelAndMessageContaining(String level, String keyword);
    // 新增：keyword + level + 时间 三者组合
    List<LogDocument> findByLevelAndMessageContainingAndTimestampBetween(
            String level, String keyword, LocalDateTime startTime, LocalDateTime endTime);
    boolean existsById(Long id);
    long count();
    // 查最新一条日志（按时间戳倒序取第一条）
    LogDocument findTopByOrderByTimestampDesc();
}