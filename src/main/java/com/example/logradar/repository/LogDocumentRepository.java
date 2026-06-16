package com.example.logradar.repository;

import com.example.logradar.entity.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface LogDocumentRepository extends ElasticsearchRepository<LogDocument, Long> {
    List<LogDocument> findByLevel(String level);
    List<LogDocument> findByMessageContaining(String keyword);
}