// mapper/LogMessageMapper.java
package com.example.logradar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.logradar.entity.LogMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogMessageMapper extends BaseMapper<LogMessage> {
}