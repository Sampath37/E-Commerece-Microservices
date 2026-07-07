package com.example.ecommerce.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut that matches all RestControllers, Controllers, and Services.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)" +
            " || within(@org.springframework.stereotype.Controller *)" +
            " || within(@org.springframework.stereotype.Service *)")
    public void springBeanPointcut() {
        // Method is empty as this is just a Pointcut
    }

    /**
     * Pointcut that matches all beans in the application's main packages.
     */
    @Pointcut("within(com.example.ecommerce..*)")
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut
    }

    /**
     * Advice that logs when a method is entered, its execution time, and any exceptions thrown.
     */
    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String componentName = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with argument[s] = {}", componentName, methodName, Arrays.toString(joinPoint.getArgs()));
        } else {
            log.info("Executing {}.{}", componentName, methodName);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() with result = {}", componentName, methodName, result);
            }
            log.info("Finished {}.{} in {} ms", componentName, methodName, stopWatch.getTotalTimeMillis());
            
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument: {} in {}.{}()", Arrays.toString(joinPoint.getArgs()), componentName, methodName, e);
            throw e;
        } catch (Exception e) {
            log.error("Exception in {}.{}() with cause = {}", componentName, methodName, e.getCause() != null ? e.getCause() : "NULL", e);
            throw e;
        }
    }
}
