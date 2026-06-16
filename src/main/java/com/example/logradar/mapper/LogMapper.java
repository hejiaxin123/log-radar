package com.example.logradar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.logradar.entity.LogRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogMapper extends BaseMapper<LogRecord> {
}