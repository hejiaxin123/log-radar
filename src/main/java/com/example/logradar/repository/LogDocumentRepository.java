package com.example.logradar.repository;

import com.example.logradar.entity.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogDocumentRepository extends ElasticsearchRepository<LogDocument, Long> {
    List<LogDocument> findByLevel(String level);
    List<LogDocument> findByMessageContaining(String keyword);
    // LogDocumentRepository.java
    List<LogDocument> findByLevelAndTimestampBetween(String level, LocalDateTime startTime, LocalDateTime endTime);
    List<LogDocument> findByMessageContainingAndTimestampBetween(String keyword, LocalDateTime startTime, LocalDateTime endTime);
    boolean existsById(Long id);
    long count();
}