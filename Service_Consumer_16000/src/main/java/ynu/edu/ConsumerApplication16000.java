package ynu.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import ynu.edu.config.RandomLoadBalancerConfig;


@SpringBootApplication
@EnableFeignClients
@LoadBalancerClient(name = "provider-service", configuration = RandomLoadBalancerConfig.class)
// 配置使用随机负载均衡策略
public class ConsumerApplication16000 {
    @Bean
    @LoadBalanced
//    用于负载均衡，加在RestTemplate上
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication16000.class, args);
    }
}
