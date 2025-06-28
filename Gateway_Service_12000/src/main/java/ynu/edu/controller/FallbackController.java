package ynu.edu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 用户服务降级响应
     */
    @GetMapping("/user")
    public Map<String, Object> userFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "服务熔断");
        response.put("message", "用户服务暂时不可用，请稍后重试");
        response.put("code", 503);
        response.put("fallback", true);
        return response;
    }

    /**
     * 提供者服务降级响应
     */
    @GetMapping("/provider")
    public Map<String, Object> providerFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "服务熔断");
        response.put("message", "提供者服务暂时不可用，请稍后重试");
        response.put("code", 503);
        response.put("fallback", true);
        return response;
    }

    /**
     * 负载均衡测试降级响应
     */
    @GetMapping("/loadbalance")
    public Map<String, Object> loadBalanceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "服务熔断");
        response.put("message", "负载均衡测试服务暂时不可用");
        response.put("servicePort", "fallback");
        response.put("serviceName", "Fallback-Service");
        response.put("fallback", true);
        return response;
    }
} 