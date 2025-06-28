package ynu.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/gateway")
public class LoadBalanceTestController {

    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 获取Gateway服务信息
     */
    @GetMapping("/info")
    public Map<String, Object> getGatewayInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("gatewayPort", "12000");
        info.put("serviceName", "Gateway-Service");
        info.put("message", "网关服务运行正常");
        info.put("timestamp", System.currentTimeMillis());
        
        // 获取已注册的服务实例信息
        List<ServiceInstance> providerInstances = discoveryClient.getInstances("provider-service");
        List<ServiceInstance> consumerInstances = discoveryClient.getInstances("consumer-service");
        
        info.put("providerInstances", providerInstances.size());
        info.put("consumerInstances", consumerInstances.size());
        info.put("providerServices", providerInstances);
        
        return info;
    }

    /**
     * 手动测试负载均衡 - 直接调用provider服务
     */
    @GetMapping("/testLoadBalance")
    public Mono<Map> testLoadBalance() {
        List<ServiceInstance> instances = discoveryClient.getInstances("provider-service");
        
        if (instances.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "没有找到可用的provider-service实例");
            return Mono.just(error);
        }

        // 随机选择一个实例（模拟负载均衡）
        ServiceInstance instance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
        String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/user/loadBalanceTest";

        return WebClient.create()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> {
                    response.put("selectedInstance", instance.getHost() + ":" + instance.getPort());
                    response.put("totalInstances", instances.size());
                });
    }

    /**
     * 获取所有注册的服务实例
     */
    @GetMapping("/services")
    public Map<String, Object> getAllServices() {
        Map<String, Object> services = new HashMap<>();
        
        List<String> serviceNames = discoveryClient.getServices();
        for (String serviceName : serviceNames) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            services.put(serviceName, instances);
        }
        
        return services;
    }
} 