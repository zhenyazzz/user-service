package com.innowise.internship.userservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.innowise.internship.userservice.service..*)")
    public void serviceLayer() {}

    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        String args = Arrays.stream(joinPoint.getArgs())
                .map(this::summarizeObject)
                .collect(Collectors.joining(", "));
        
        log.debug("--> Invoke: {}. Arguments: [{}]", 
                joinPoint.getSignature().toShortString(), args);
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        log.debug("<-- Return: {}. Result: {}", 
                joinPoint.getSignature().toShortString(), summarizeObject(result));
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("!!! Exception in {}: {}", 
                joinPoint.getSignature().toShortString(), e.getMessage());
    }

    private String summarizeObject(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof Page<?> page) {
            return String.format("Page(size=%d, total=%d, number=%d, content_size=%d)",
                    page.getSize(), page.getTotalElements(), page.getNumber(), page.getContent().size());
        }

        if (obj instanceof Collection<?> col) {
            return "Collection(size=" + col.size() + ")";
        }

        return obj.toString();
    }
}