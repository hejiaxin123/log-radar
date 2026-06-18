package com.example.logradar.parser;

import com.example.logradar.entity.LogRecord;

public interface LogParser {
    LogRecord parse(String raw);
}