package com.example.logradar.aop;

import com.example.logradar.entity.SlowQueryLog;
import com.example.logradar.mapper.SlowQueryLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SlowQueryAspect {

    private final SlowQueryLogMapper slowQueryLogMapper;

    public SlowQueryAspect(SlowQueryLogMapper slowQueryLogMapper) {
        this.slowQueryLogMapper = slowQueryLogMapper;
    }

    @Around("execution(* com.example.logradar.service.LogService.search(..))")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long time = System.currentTimeMillis() - start;

        if (time > 500) {
            SlowQueryLog log = new SlowQueryLog();
            log.setMethodName(joinPoint.getSignature().getName());
            log.setExecutionTimeMs(time);
            slowQueryLogMapper.insert(log);
            System.out.println("慢查询记录：" + joinPoint.getSignature().getName() + " 耗时 " + time + "ms");
        }
        return result;
    }
}