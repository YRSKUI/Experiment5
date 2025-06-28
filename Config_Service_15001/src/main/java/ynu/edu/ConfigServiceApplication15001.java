package ynu.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication15001 {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication15001.class, args);
    }
} 