package ynu.edu.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 断路器、隔离器和限流器配置类
 * 用于配置和监控断路器实例、隔离器实例和限流器实例
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * 获取断路器注册表
     * 可以用于监控断路器状态
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    /**
     * 获取隔离器注册表
     * 可以用于监控隔离器状态
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }

    /**
     * 获取限流器注册表
     * 可以用于监控限流器状态
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    /**
     * 监听断路器状态变化的事件
     */
    public void configureCircuitBreakerEvents(CircuitBreakerRegistry registry) {
        
        // 监听断路器A的状态变化
        CircuitBreaker circuitBreakerA = registry.circuitBreaker("circuitBreakerA");
        circuitBreakerA.getEventPublisher()
                .onStateTransition(event -> 
                    System.out.println("断路器A状态变化: " + event.getStateTransition()));
        circuitBreakerA.getEventPublisher()
                .onFailureRateExceeded(event -> 
                    System.out.println("断路器A失败率超过阈值: " + event.getFailureRate()));

        // 监听断路器B的状态变化
        CircuitBreaker circuitBreakerB = registry.circuitBreaker("circuitBreakerB");
        circuitBreakerB.getEventPublisher()
                .onStateTransition(event -> 
                    System.out.println("断路器B状态变化: " + event.getStateTransition()));
        circuitBreakerB.getEventPublisher()
                .onFailureRateExceeded(event -> 
                    System.out.println("断路器B失败率超过阈值: " + event.getFailureRate()));
        circuitBreakerB.getEventPublisher()
                .onSlowCallRateExceeded(event -> 
                    System.out.println("断路器B慢调用率超过阈值: " + event.getSlowCallRate()));
    }

    /**
     * 监听隔离器状态变化的事件
     */
    public void configureBulkheadEvents(BulkheadRegistry registry) {
        
        // 监听隔离器的状态变化
        Bulkhead bulkhead = registry.bulkhead("userServiceBulkhead");
        bulkhead.getEventPublisher()
                .onCallPermitted(event -> 
                    System.out.println("隔离器允许调用: " + event.getBulkheadName()));
        bulkhead.getEventPublisher()
                .onCallRejected(event -> 
                    System.out.println("隔离器拒绝调用: " + event.getBulkheadName()));
        bulkhead.getEventPublisher()
                .onCallFinished(event -> 
                    System.out.println("隔离器调用完成: " + event.getBulkheadName()));
    }

    /**
     * 监听限流器状态变化的事件
     */
    public void configureRateLimiterEvents(RateLimiterRegistry registry) {
        
        // 监听限流器的状态变化
        RateLimiter rateLimiter = registry.rateLimiter("userServiceRateLimiter");
        rateLimiter.getEventPublisher()
                .onSuccess(event -> 
                    System.out.println("限流器允许请求: " + event.getRateLimiterName() + 
                    ", 可用许可: " + event.getNumberOfPermits()));
        rateLimiter.getEventPublisher()
                .onFailure(event -> 
                    System.out.println("限流器拒绝请求: " + event.getRateLimiterName() + 
                    ", 原因: " + event.getCreationTime()));
    }
} 