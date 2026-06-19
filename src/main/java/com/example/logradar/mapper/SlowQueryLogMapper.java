package com.example.logradar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.logradar.entity.SlowQueryLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlowQueryLogMapper extends BaseMapper<SlowQueryLog> {
}