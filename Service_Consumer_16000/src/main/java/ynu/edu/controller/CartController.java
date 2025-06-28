package ynu.edu.controller;

import com.netflix.appinfo.InstanceInfo;
//import com.netflix.discovery.DiscoveryClient;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.annotation.Resource;
//import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
//用原版的
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ynu.edu.entity.Cart;
import ynu.edu.entity.User;
import ynu.edu.feign.UserFeignClient;
import ynu.edu.feign.UserFeignClientA;
import ynu.edu.feign.UserFeignClientB;
import ynu.edu.feign.UserFeignClientWithBulkhead;
import ynu.edu.feign.UserFeignClientWithRateLimit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/cart")
@RefreshScope
public class CartController {

    @Resource
    private RestTemplate restTemplate;

    // 注入FeignClient接口
    @Resource
    private UserFeignClient userFeignClient;

//    @Resource
//    private DiscoveryClient discoveryClient;


    @RequestMapping("/getCartById/{userId}")
    public Cart getCartById(@PathVariable("userId") Integer userId) {
//        List<ServiceInstance> instancesList = discoveryClient.getInstances("product-provider");
//        //使用discoveryClient获取product-provider的实例
//        ServiceInstance instance = instancesList.get(1);
//        //这里可以用来做负载均衡


        Cart cart = new Cart();
        List<String> goods = new ArrayList<>();
        goods.add("电池");
        goods.add("充电器");
        cart.setGoodsList(goods);

        User u = restTemplate.getForObject("http://provider-service/user/getUserById/"+userId.toString(), User.class);
        cart.setUser(u);
        return cart;
    }

    // ================== 负载均衡效果展示 ==================
    
    /**
     * 展示RestTemplate随机负载均衡效果
     * 连续调用多次可以看到请求被随机分发到不同的服务实例（11000和11001）
     */
    @GetMapping("/loadbalance/resttemplate/test")
    public String testRestTemplateLoadBalance() {
        try {
            User user = restTemplate.getForObject("http://provider-service/user/getUserById/1", User.class);
            return "RestTemplate随机负载均衡测试 - 服务实例返回: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            return "RestTemplate随机负载均衡测试失败: " + e.getMessage();
        }
    }

    /**
     * 展示FeignClient随机负载均衡效果
     * 连续调用多次可以看到请求被随机分发到不同的服务实例（11000和11001）
     */
    @GetMapping("/loadbalance/feign/test")
    public String testFeignLoadBalance() {
        try {
            User user = userFeignClient.getUserById(1);
            return "FeignClient随机负载均衡测试 - 服务实例返回: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            return "FeignClient随机负载均衡测试失败: " + e.getMessage();
        }
    }

    /**
     * 批量测试RestTemplate随机负载均衡效果
     * 一次调用展示多次请求的随机分发情况
     */
    @GetMapping("/loadbalance/resttemplate/batch/{count}")
    public String batchTestRestTemplateLoadBalance(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        result.append("RestTemplate批量随机负载均衡测试 (").append(count).append("次调用):\n");
        
        for (int i = 1; i <= count; i++) {
            try {
                User user = restTemplate.getForObject("http://provider-service/user/getUserById/1", User.class);
                result.append("第").append(i).append("次调用: ").append(user.getUserName()).append("\n");
            } catch (Exception e) {
                result.append("第").append(i).append("次调用失败: ").append(e.getMessage()).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * 批量测试FeignClient随机负载均衡效果
     * 一次调用展示多次请求的随机分发情况
     */
    @GetMapping("/loadbalance/feign/batch/{count}")
    public String batchTestFeignLoadBalance(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        result.append("FeignClient批量随机负载均衡测试 (").append(count).append("次调用):\n");
        
        for (int i = 1; i <= count; i++) {
            try {
                User user = userFeignClient.getUserById(1);
                result.append("第").append(i).append("次调用: ").append(user.getUserName()).append("\n");
            } catch (Exception e) {
                result.append("第").append(i).append("次调用失败: ").append(e.getMessage()).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * 统计测试随机负载均衡分发情况
     * 展示请求在不同服务实例间的分布统计
     */
    @GetMapping("/loadbalance/random/statistics/{count}")
    public String testRandomLoadBalanceStatistics(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        int port11000Count = 0;
        int port11001Count = 0;
        
        result.append("随机负载均衡统计测试 (").append(count).append("次调用):\n\n");
        
        for (int i = 1; i <= count; i++) {
            try {
                User user = restTemplate.getForObject("http://provider-service/user/getUserById/1", User.class);
                if (user.getUserName().contains("11000")) {
                    port11000Count++;
                } else if (user.getUserName().contains("11001")) {
                    port11001Count++;
                }
            } catch (Exception e) {
                result.append("第").append(i).append("次调用失败: ").append(e.getMessage()).append("\n");
            }
        }
        
        result.append("=== 统计结果 ===\n");
        result.append("端口11000服务实例调用次数: ").append(port11000Count).append("\n");
        result.append("端口11001服务实例调用次数: ").append(port11001Count).append("\n");
        result.append("总调用次数: ").append(count).append("\n");
        result.append("11000实例分配比例: ").append(String.format("%.2f", (double)port11000Count / count * 100)).append("%\n");
        result.append("11001实例分配比例: ").append(String.format("%.2f", (double)port11001Count / count * 100)).append("%\n");
        result.append("\n随机策略说明: 由于采用随机分发策略，每次运行的分配比例可能不同，这是正常现象。");
        
        return result.toString();
    }

    // ================== RestTemplate 方法演示 ==================
    
    // 演示RestTemplate的GET方法
    @GetMapping("/demo/get/{userId}")
    public String demoGetUser(@PathVariable("userId") Integer userId) {
        try {
            User user = restTemplate.getForObject("http://provider-service/user/getUserById/" + userId, User.class);
            return "GET方法调用成功！获取到用户: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            return "GET方法调用失败: " + e.getMessage();
        }
    }

    // 演示RestTemplate的POST方法
    @PostMapping("/demo/post")
    public String demoCreateUser(@RequestBody User user) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<User> request = new HttpEntity<>(user, headers);
            
            String result = restTemplate.postForObject("http://provider-service/user/createUser", request, String.class);
            return "POST方法调用成功！" + result;
        } catch (Exception e) {
            return "POST方法调用失败: " + e.getMessage();
        }
    }

    // 演示RestTemplate的PUT方法
    @PutMapping("/demo/put/{userId}")
    public String demoUpdateUser(@PathVariable("userId") Integer userId, @RequestBody User user) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<User> request = new HttpEntity<>(user, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                "http://provider-service/user/updateUser/" + userId, 
                HttpMethod.PUT, 
                request, 
                String.class
            );
            return "PUT方法调用成功！" + response.getBody();
        } catch (Exception e) {
            return "PUT方法调用失败: " + e.getMessage();
        }
    }

    // 演示RestTemplate的DELETE方法
    @DeleteMapping("/demo/delete/{userId}")
    public String demoDeleteUser(@PathVariable("userId") Integer userId) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "http://provider-service/user/deleteUser/" + userId, 
                HttpMethod.DELETE, 
                null, 
                String.class
            );
            return "DELETE方法调用成功！" + response.getBody();
        } catch (Exception e) {
            return "DELETE方法调用失败: " + e.getMessage();
        }
    }

    // 获取所有用户（测试用）
    @GetMapping("/demo/getAllUsers")
    public String demoGetAllUsers() {
        try {
            Map<Integer, User> users = restTemplate.getForObject("http://provider-service/user/getAllUsers", Map.class);
            return "GET All Users调用成功！用户列表: " + users.toString();
        } catch (Exception e) {
            return "GET All Users调用失败: " + e.getMessage();
        }
    }

    // ================== FeignClient 方法演示 ==================

    // 演示FeignClient的GET方法
    @GetMapping("/feign/get/{userId}")
    public String feignGetUser(@PathVariable("userId") Integer userId) {
        try {
            User user = userFeignClient.getUserById(userId);
            return "Feign GET方法调用成功！获取到用户: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            return "Feign GET方法调用失败: " + e.getMessage();
        }
    }

    // 演示FeignClient的POST方法
    @PostMapping("/feign/post")
    public String feignCreateUser(@RequestBody User user) {
        try {
            String result = userFeignClient.createUser(user);
            return "Feign POST方法调用成功！" + result;
        } catch (Exception e) {
            return "Feign POST方法调用失败: " + e.getMessage();
        }
    }

    // 演示FeignClient的PUT方法
    @PutMapping("/feign/put/{userId}")
    public String feignUpdateUser(@PathVariable("userId") Integer userId, @RequestBody User user) {
        try {
            String result = userFeignClient.updateUser(userId, user);
            return "Feign PUT方法调用成功！" + result;
        } catch (Exception e) {
            return "Feign PUT方法调用失败: " + e.getMessage();
        }
    }

    // 演示FeignClient的DELETE方法
    @DeleteMapping("/feign/delete/{userId}")
    public String feignDeleteUser(@PathVariable("userId") Integer userId) {
        try {
            String result = userFeignClient.deleteUser(userId);
            return "Feign DELETE方法调用成功！" + result;
        } catch (Exception e) {
            return "Feign DELETE方法调用失败: " + e.getMessage();
        }
    }

    // 演示FeignClient获取所有用户
    @GetMapping("/feign/getAllUsers")
    public String feignGetAllUsers() {
        try {
            Map<Integer, User> users = userFeignClient.getAllUsers();
            return "FeignClient获取所有用户成功！用户数量: " + users.size() + ", 用户列表: " + users.toString();
        } catch (Exception e) {
            return "FeignClient获取所有用户失败: " + e.getMessage();
        }
    }

    // ================== 断路器演示方法 ==================
    
    /**
     * 使用断路器实例A进行服务调用
     * 配置：失败率阈值30%，时间窗口10秒，最小请求数5
     */
    @GetMapping("/circuitbreaker/instanceA/{userId}")
    @CircuitBreaker(name = "circuitBreakerA", fallbackMethod = "fallbackMethodA")
    public String testCircuitBreakerA(@PathVariable("userId") Integer userId) {
        try {
            User user = userFeignClient.getUserById(userId);
            return "断路器实例A调用成功！获取到用户: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            throw new RuntimeException("服务调用异常: " + e.getMessage());
        }
    }

    /**
     * 断路器实例A的降级方法
     */
    public String fallbackMethodA(Integer userId, Exception ex) {
        return "断路器实例A已开启！服务暂时不可用，用户ID: " + userId + 
               "，错误信息: " + ex.getMessage() + 
               "。配置信息：失败率阈值30%，滑动窗口10秒，最小请求数5";
    }

    /**
     * 使用断路器实例B进行服务调用
     * 配置：失败率阈值50%，慢调用阈值30%，慢调用时间2秒，时间窗口10秒，最小请求数5
     */
    @GetMapping("/circuitbreaker/instanceB/{userId}")
    @CircuitBreaker(name = "circuitBreakerB", fallbackMethod = "fallbackMethodB")
    public String testCircuitBreakerB(@PathVariable("userId") Integer userId) {
        try {
            // 模拟慢调用：随机延迟
            if (Math.random() > 0.7) {
                Thread.sleep(3000); // 3秒延迟，超过慢调用阈值2秒
            }
            
            User user = userFeignClient.getUserById(userId);
            return "断路器实例B调用成功！获取到用户: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            throw new RuntimeException("服务调用异常: " + e.getMessage());
        }
    }

    /**
     * 断路器实例B的降级方法
     */
    public String fallbackMethodB(Integer userId, Exception ex) {
        return "断路器实例B已开启！服务暂时不可用，用户ID: " + userId + 
               "，错误信息: " + ex.getMessage() + 
               "。配置信息：失败率阈值50%，慢调用阈值30%，慢调用时间2秒，滑动窗口10秒，最小请求数5";
    }

    /**
     * 批量测试断路器实例A的触发效果
     */
    @GetMapping("/circuitbreaker/instanceA/batch/{count}")
    public String batchTestCircuitBreakerA(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        result.append("断路器实例A批量测试 (").append(count).append("次调用):\n\n");
        
        int successCount = 0;
        int fallbackCount = 0;
        
        for (int i = 1; i <= count; i++) {
            try {
                String response = testCircuitBreakerA(1);
                if (response.contains("调用成功")) {
                    successCount++;
                } else {
                    fallbackCount++;
                }
                result.append("第").append(i).append("次: ").append(response).append("\n");
            } catch (Exception e) {
                fallbackCount++;
                result.append("第").append(i).append("次: 调用异常 - ").append(e.getMessage()).append("\n");
            }
            
            // 添加小延迟避免过快调用
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        result.append("\n=== 统计结果 ===\n");
        result.append("成功调用次数: ").append(successCount).append("\n");
        result.append("降级调用次数: ").append(fallbackCount).append("\n");
        result.append("失败率: ").append(String.format("%.2f", (double)fallbackCount / count * 100)).append("%\n");
        result.append("断路器A配置: 失败率阈值30%，当失败率超过30%时断路器将开启");
        
        return result.toString();
    }

    /**
     * 批量测试断路器实例B的触发效果（包含慢调用检测）
     */
    @GetMapping("/circuitbreaker/instanceB/batch/{count}")
    public String batchTestCircuitBreakerB(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        result.append("断路器实例B批量测试 (").append(count).append("次调用):\n\n");
        
        int successCount = 0;
        int fallbackCount = 0;
        int slowCallCount = 0;
        
        for (int i = 1; i <= count; i++) {
            long startTime = System.currentTimeMillis();
            try {
                String response = testCircuitBreakerB(1);
                long duration = System.currentTimeMillis() - startTime;
                
                if (duration > 2000) {
                    slowCallCount++;
                    result.append("第").append(i).append("次: [慢调用-").append(duration).append("ms] ");
                } else {
                    result.append("第").append(i).append("次: [").append(duration).append("ms] ");
                }
                
                if (response.contains("调用成功")) {
                    successCount++;
                } else {
                    fallbackCount++;
                }
                result.append(response).append("\n");
            } catch (Exception e) {
                fallbackCount++;
                long duration = System.currentTimeMillis() - startTime;
                result.append("第").append(i).append("次: [").append(duration).append("ms] 调用异常 - ").append(e.getMessage()).append("\n");
            }
            
            // 添加小延迟避免过快调用
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        result.append("\n=== 统计结果 ===\n");
        result.append("成功调用次数: ").append(successCount).append("\n");
        result.append("降级调用次数: ").append(fallbackCount).append("\n");
        result.append("慢调用次数: ").append(slowCallCount).append("\n");
        result.append("失败率: ").append(String.format("%.2f", (double)fallbackCount / count * 100)).append("%\n");
        result.append("慢调用率: ").append(String.format("%.2f", (double)slowCallCount / count * 100)).append("%\n");
        result.append("断路器B配置: 失败率阈值50%，慢调用阈值30%，当失败率或慢调用率超过阈值时断路器将开启");
        
        return result.toString();
    }

    /**
     * 模拟服务异常来测试断路器
     */
    @GetMapping("/circuitbreaker/simulate/error/{instance}")
    @CircuitBreaker(name = "circuitBreakerA", fallbackMethod = "fallbackMethodA")
    public String simulateError(@PathVariable("instance") String instance) {
        // 总是抛出异常来触发断路器
        throw new RuntimeException("模拟服务异常 - 用于测试断路器" + instance + "的开启");
    }

    // ================== OpenFeign断路器演示方法 ==================
    
    // 注入使用断路器A的FeignClient
    @Resource
    private UserFeignClientA userFeignClientA;
    
    // 注入使用断路器B的FeignClient
    @Resource
    private UserFeignClientB userFeignClientB;

    /**
     * 使用带断路器A保护的FeignClient进行服务调用
     */
    @GetMapping("/feign/circuitbreakerA/getUserById/{userId}")
    @CircuitBreaker(name = "circuitBreakerA", fallbackMethod = "feignFallbackMethodA")
    public String testFeignWithCircuitBreakerA(@PathVariable("userId") Integer userId) {
        try {
            User user = userFeignClientA.getUserById(userId);
            return "【FeignClient A + 断路器A】调用成功！获取到用户: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            throw new RuntimeException("FeignClient A 服务调用异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient A + 断路器A的降级方法
     */
    public String feignFallbackMethodA(Integer userId, Exception ex) {
        return "【FeignClient A + 断路器A 双重保护降级】服务暂时不可用，用户ID: " + userId + 
               "，错误信息: " + ex.getMessage() + 
               "。配置：FeignClient内置fallback + 断路器A（失败率阈值30%）";
    }

    /**
     * 使用带断路器B保护的FeignClient进行服务调用
     */
    @GetMapping("/feign/circuitbreakerB/getUserById/{userId}")
    @CircuitBreaker(name = "circuitBreakerB", fallbackMethod = "feignFallbackMethodB")
    public String testFeignWithCircuitBreakerB(@PathVariable("userId") Integer userId) {
        try {
            // 随机模拟慢调用
            if (Math.random() > 0.8) {
                Thread.sleep(2500); // 2.5秒延迟，超过慢调用阈值2秒
            }
            
            User user = userFeignClientB.getUserById(userId);
            return "【FeignClient B + 断路器B】调用成功！获取到用户: " + user.getUserName() + " (ID: " + user.getUserId() + ")";
        } catch (Exception e) {
            throw new RuntimeException("FeignClient B 服务调用异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient B + 断路器B的降级方法
     */
    public String feignFallbackMethodB(Integer userId, Exception ex) {
        return "【FeignClient B + 断路器B 双重保护降级】服务暂时不可用，用户ID: " + userId + 
               "，错误信息: " + ex.getMessage() + 
               "。配置：FeignClient内置fallback + 断路器B（失败率阈值50%，慢调用阈值30%）";
    }

    /**
     * 测试FeignClient A的所有CRUD操作
     */
    @GetMapping("/feign/circuitbreakerA/test/all")
    @CircuitBreaker(name = "circuitBreakerA", fallbackMethod = "feignCrudFallbackA")
    public String testFeignCrudWithCircuitBreakerA() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("【FeignClient A + 断路器A】CRUD操作测试:\n\n");

            // 测试GET
            User user = userFeignClientA.getUserById(1);
            result.append("GET操作: ").append(user.getUserName()).append("\n");

            // 测试POST
            User newUser = new User();
            newUser.setUserName("测试用户A");
            newUser.setPassWord("123456");
            newUser.setAge(25);
            String createResult = userFeignClientA.createUser(newUser);
            result.append("POST操作: ").append(createResult).append("\n");

            // 测试PUT
            User updateUser = new User();
            updateUser.setUserName("更新用户A");
            updateUser.setPassWord("654321");
            updateUser.setAge(30);
            String updateResult = userFeignClientA.updateUser(1, updateUser);
            result.append("PUT操作: ").append(updateResult).append("\n");

            // 测试DELETE
            String deleteResult = userFeignClientA.deleteUser(1);
            result.append("DELETE操作: ").append(deleteResult).append("\n");

            // 测试GET ALL
            Map<Integer, User> allUsers = userFeignClientA.getAllUsers();
            result.append("GET ALL操作: 获取到").append(allUsers.size()).append("个用户\n");

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("FeignClient A CRUD操作异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient A CRUD操作的降级方法
     */
    public String feignCrudFallbackA(Exception ex) {
        return "【FeignClient A + 断路器A】CRUD操作服务暂时不可用，错误信息: " + ex.getMessage() + 
               "。所有用户相关操作均已降级保护。";
    }

    /**
     * 测试FeignClient B的所有CRUD操作
     */
    @GetMapping("/feign/circuitbreakerB/test/all")
    @CircuitBreaker(name = "circuitBreakerB", fallbackMethod = "feignCrudFallbackB")
    public String testFeignCrudWithCircuitBreakerB() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("【FeignClient B + 断路器B】CRUD操作测试:\n\n");

            // 随机添加延迟模拟慢调用
            if (Math.random() > 0.7) {
                Thread.sleep(2200); // 超过2秒慢调用阈值
                result.append("[慢调用模拟] ");
            }

            // 测试GET
            User user = userFeignClientB.getUserById(1);
            result.append("GET操作: ").append(user.getUserName()).append("\n");

            // 测试POST
            User newUser = new User();
            newUser.setUserName("测试用户B");
            newUser.setPassWord("123456");
            newUser.setAge(28);
            String createResult = userFeignClientB.createUser(newUser);
            result.append("POST操作: ").append(createResult).append("\n");

            // 测试PUT
            User updateUser = new User();
            updateUser.setUserName("更新用户B");
            updateUser.setPassWord("654321");
            updateUser.setAge(32);
            String updateResult = userFeignClientB.updateUser(1, updateUser);
            result.append("PUT操作: ").append(updateResult).append("\n");

            // 测试DELETE
            String deleteResult = userFeignClientB.deleteUser(1);
            result.append("DELETE操作: ").append(deleteResult).append("\n");

            // 测试GET ALL
            Map<Integer, User> allUsers = userFeignClientB.getAllUsers();
            result.append("GET ALL操作: 获取到").append(allUsers.size()).append("个用户\n");

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("FeignClient B CRUD操作异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient B CRUD操作的降级方法
     */
    public String feignCrudFallbackB(Exception ex) {
        return "【FeignClient B + 断路器B】CRUD操作服务暂时不可用，错误信息: " + ex.getMessage() + 
               "。所有用户相关操作均已降级保护。包含慢调用检测功能。";
    }

    /**
     * 批量测试FeignClient A + 断路器A的双重保护效果
     */
    @GetMapping("/feign/circuitbreakerA/batch/{count}")
    public String batchTestFeignWithCircuitBreakerA(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        result.append("【FeignClient A + 断路器A】批量测试 (").append(count).append("次调用):\n\n");
        
        int successCount = 0;
        int feignFallbackCount = 0;
        int circuitBreakerFallbackCount = 0;
        
        for (int i = 1; i <= count; i++) {
            try {
                String response = testFeignWithCircuitBreakerA(1);
                if (response.contains("调用成功")) {
                    successCount++;
                } else if (response.contains("FeignClient A + 断路器A 双重保护降级")) {
                    circuitBreakerFallbackCount++;
                } else if (response.contains("断路器A降级")) {
                    feignFallbackCount++;
                }
                result.append("第").append(i).append("次: ").append(response).append("\n");
            } catch (Exception e) {
                result.append("第").append(i).append("次: 调用异常 - ").append(e.getMessage()).append("\n");
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        result.append("\n=== 统计结果 ===\n");
        result.append("成功调用次数: ").append(successCount).append("\n");
        result.append("FeignClient降级次数: ").append(feignFallbackCount).append("\n");
        result.append("断路器降级次数: ").append(circuitBreakerFallbackCount).append("\n");
        result.append("FeignClient A + 断路器A提供双重保护，确保服务的高可用性");
        
        return result.toString();
    }

    /**
     * 批量测试FeignClient B + 断路器B的双重保护效果
     */
    @GetMapping("/feign/circuitbreakerB/batch/{count}")
    public String batchTestFeignWithCircuitBreakerB(@PathVariable("count") Integer count) {
        StringBuilder result = new StringBuilder();
        result.append("【FeignClient B + 断路器B】批量测试 (").append(count).append("次调用):\n\n");
        
        int successCount = 0;
        int feignFallbackCount = 0;
        int circuitBreakerFallbackCount = 0;
        int slowCallCount = 0;
        
        for (int i = 1; i <= count; i++) {
            long startTime = System.currentTimeMillis();
            try {
                String response = testFeignWithCircuitBreakerB(1);
                long duration = System.currentTimeMillis() - startTime;
                
                if (duration > 2000) {
                    slowCallCount++;
                    result.append("第").append(i).append("次: [慢调用-").append(duration).append("ms] ");
                } else {
                    result.append("第").append(i).append("次: [").append(duration).append("ms] ");
                }
                
                if (response.contains("调用成功")) {
                    successCount++;
                } else if (response.contains("FeignClient B + 断路器B 双重保护降级")) {
                    circuitBreakerFallbackCount++;
                } else if (response.contains("断路器B降级")) {
                    feignFallbackCount++;
                }
                result.append(response).append("\n");
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                result.append("第").append(i).append("次: [").append(duration).append("ms] 调用异常 - ").append(e.getMessage()).append("\n");
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        result.append("\n=== 统计结果 ===\n");
        result.append("成功调用次数: ").append(successCount).append("\n");
        result.append("FeignClient降级次数: ").append(feignFallbackCount).append("\n");
        result.append("断路器降级次数: ").append(circuitBreakerFallbackCount).append("\n");
        result.append("慢调用次数: ").append(slowCallCount).append("\n");
        result.append("慢调用率: ").append(String.format("%.2f", (double)slowCallCount / count * 100)).append("%\n");
        result.append("FeignClient B + 断路器B提供双重保护，包含失败率和慢调用率双重检测");
        
        return result.toString();
    }

    // ================== 隔离器演示方法 ==================
    
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    /**
     * 使用隔离器进行并发控制的服务调用
     * 配置：最大并发数10，最大等待时间20ms
     */
    @GetMapping("/bulkhead/test/{userId}")
    @Bulkhead(name = "userServiceBulkhead", fallbackMethod = "bulkheadFallback")
    public String testWithBulkhead(@PathVariable("userId") Integer userId) {
        int requestId = requestCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 模拟业务处理时间（1-3秒随机）
            int processingTime = 1000 + (int)(Math.random() * 2000);
            Thread.sleep(processingTime);
            
            User user = userFeignClient.getUserById(userId);
            return String.format("【隔离器保护】请求#%d 在 %s 成功处理，耗时%dms，获取用户: %s (ID: %d)", 
                    requestId, timestamp, processingTime, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            return String.format("【隔离器保护】请求#%d 在 %s 处理失败: %s", 
                    requestId, timestamp, e.getMessage());
        }
    }

    /**
     * 隔离器的降级方法
     */
    public String bulkheadFallback(Integer userId, Exception ex) {
        int requestId = requestCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        return String.format("【隔离器降级】请求#%d 在 %s 被拒绝，用户ID: %d，原因: %s。" +
                "配置：最大并发数10，最大等待时间20ms", 
                requestId, timestamp, userId, ex.getMessage());
    }

    /**
     * 快速服务调用（用于对比隔离器效果）
     */
    @GetMapping("/bulkhead/fast/{userId}")
    @Bulkhead(name = "userServiceBulkhead", fallbackMethod = "bulkheadFallback")
    public String testFastWithBulkhead(@PathVariable("userId") Integer userId) {
        int requestId = requestCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 快速处理（100ms）
            Thread.sleep(100);
            
            User user = userFeignClient.getUserById(userId);
            return String.format("【隔离器保护-快速】请求#%d 在 %s 快速处理完成，获取用户: %s (ID: %d)", 
                    requestId, timestamp, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            return String.format("【隔离器保护-快速】请求#%d 在 %s 处理失败: %s", 
                    requestId, timestamp, e.getMessage());
        }
    }

    /**
     * 模拟高并发场景测试隔离器效果
     */
    @GetMapping("/bulkhead/concurrent/test/{threadCount}")
    public String testConcurrentWithBulkhead(@PathVariable("threadCount") Integer threadCount) {
        if (threadCount > 50) {
            return "为了系统安全，线程数量限制在50以内";
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        // 创建多个并发请求
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return testWithBulkhead(1);
                } catch (Exception e) {
                    return "并发请求异常: " + e.getMessage();
                }
            });
            futures.add(future);
        }
        
        // 等待所有请求完成
        List<String> results = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("Future异常: " + e.getMessage());
            }
        }
        
        // 统计结果
        int successCount = 0;
        int fallbackCount = 0;
        
        for (String result : results) {
            if (result.contains("成功处理")) {
                successCount++;
            } else if (result.contains("隔离器降级") || result.contains("被拒绝")) {
                fallbackCount++;
            }
        }
        
        String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("【隔离器并发测试】开始时间: %s，结束时间: %s\n", startTime, endTime));
        summary.append(String.format("总请求数: %d，成功处理: %d，隔离器拒绝: %d\n", 
                threadCount, successCount, fallbackCount));
        summary.append("配置说明：最大并发数10，最大等待时间20ms\n\n");
        
        summary.append("=== 详细结果 ===\n");
        for (int i = 0; i < results.size(); i++) {
            summary.append(String.format("线程%02d: %s\n", i+1, results.get(i)));
        }
        
        return summary.toString();
    }

    /**
     * 测试隔离器在高并发下的性能表现
     */
    @GetMapping("/bulkhead/performance/test/{requestCount}")
    public String testBulkheadPerformance(@PathVariable("requestCount") Integer requestCount) {
        if (requestCount > 100) {
            return "为了系统安全，请求数量限制在100以内";
        }

        long startTime = System.currentTimeMillis();
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // 快速发送所有请求
        for (int i = 0; i < requestCount; i++) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return testFastWithBulkhead(1);
            });
            futures.add(future);
        }
        
        // 收集结果
        List<String> results = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("性能测试异常: " + e.getMessage());
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 统计分析
        int successCount = 0;
        int fallbackCount = 0;
        
        for (String result : results) {
            if (result.contains("快速处理完成")) {
                successCount++;
            } else if (result.contains("隔离器降级")) {
                fallbackCount++;
            }
        }
        
        StringBuilder report = new StringBuilder();
        report.append("【隔离器性能测试报告】\n");
        report.append(String.format("测试参数：请求数量=%d，处理时间=100ms/请求\n", requestCount));
        report.append(String.format("配置参数：最大并发数=10，最大等待时间=20ms\n"));
        report.append(String.format("测试结果：总耗时=%dms，成功处理=%d，隔离器拒绝=%d\n", 
                totalTime, successCount, fallbackCount));
        report.append(String.format("性能指标：成功率=%.2f%%，吞吐量=%.2f请求/秒\n", 
                (double)successCount/requestCount*100, (double)successCount*1000/totalTime));
        
        double expectedTime = Math.ceil((double)requestCount / 10) * 100; // 理论最佳时间
        report.append(String.format("预期时间：%.0fms（基于10个并发槽位）\n", expectedTime));
        
        if (fallbackCount > 0) {
            report.append("\n【分析】当请求数超过隔离器容量时，超出的请求将被快速拒绝，" +
                    "避免系统过载，这是隔离器保护机制的正常表现。\n");
        }
        
        return report.toString();
    }

    /**
     * 隔离器状态监控
     */
    @GetMapping("/bulkhead/status")
    public String getBulkheadStatus() {
        return "【隔离器状态监控】\n" +
               "实例名称: userServiceBulkhead\n" +
               "最大并发数: 10\n" +
               "最大等待时间: 20ms\n" +
               "当前配置: 允许10个线程同时执行，超出的线程最多等待20ms\n" +
               "保护机制: 当并发数超过限制且等待时间超过20ms时，新请求将被快速拒绝\n" +
               "适用场景: 保护下游服务免受过多并发请求影响，确保系统稳定性";
    }

    // ================== OpenFeign隔离器演示方法 ==================
    
    // 注入使用隔离器保护的FeignClient
    @Resource
    private UserFeignClientWithBulkhead userFeignClientWithBulkhead;

    /**
     * 使用隔离器保护的FeignClient进行服务调用
     * 双重保护：@Bulkhead注解 + FeignClient内置fallback
     */
    @GetMapping("/feign/bulkhead/getUserById/{userId}")
    @Bulkhead(name = "userServiceBulkhead", fallbackMethod = "feignBulkheadFallback")
    public String testFeignWithBulkhead(@PathVariable("userId") Integer userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 模拟业务处理时间（500ms-1.5s随机）
            int processingTime = 500 + (int)(Math.random() * 1000);
            Thread.sleep(processingTime);
            
            User user = userFeignClientWithBulkhead.getUserById(userId);
            
            // 判断是否为降级响应
            if (user.getUserId() == -999) {
                return String.format("【FeignClient隔离器保护】%s 触发了FeignClient内置降级，时间：%s", 
                        user.getUserName(), timestamp);
            }
            
            return String.format("【FeignClient隔离器保护】%s 成功调用，耗时%dms，获取用户: %s (ID: %d)", 
                    timestamp, processingTime, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            throw new RuntimeException("FeignClient隔离器保护调用异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient + 隔离器的外层降级方法
     */
    public String feignBulkheadFallback(Integer userId, Exception ex) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return String.format("【FeignClient + 隔离器双重保护降级】%s 服务暂时不可用或超出并发限制，用户ID: %d，错误: %s。" +
                "配置：FeignClient内置fallback + 隔离器（最大并发数10，最大等待时间20ms）", 
                timestamp, userId, ex.getMessage());
    }

    /**
     * 快速FeignClient隔离器测试
     */
    @GetMapping("/feign/bulkhead/fast/{userId}")
    @Bulkhead(name = "userServiceBulkhead", fallbackMethod = "feignBulkheadFallback")
    public String testFeignFastWithBulkhead(@PathVariable("userId") Integer userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 快速处理（50ms）
            Thread.sleep(50);
            
            User user = userFeignClientWithBulkhead.getUserById(userId);
            
            if (user.getUserId() == -999) {
                return String.format("【FeignClient隔离器保护-快速】%s 触发了FeignClient内置降级", timestamp);
            }
            
            return String.format("【FeignClient隔离器保护-快速】%s 快速调用成功，获取用户: %s (ID: %d)", 
                    timestamp, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            throw new RuntimeException("FeignClient快速隔离器保护调用异常: " + e.getMessage());
        }
    }

    /**
     * 测试FeignClient隔离器保护的完整CRUD操作
     */
    @GetMapping("/feign/bulkhead/crud/test")
    @Bulkhead(name = "userServiceBulkhead", fallbackMethod = "feignBulkheadCrudFallback")
    public String testFeignBulkheadCrud() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            StringBuilder result = new StringBuilder();
            result.append(String.format("【FeignClient隔离器保护CRUD测试】开始时间: %s\n\n", timestamp));

            // 模拟处理时间
            Thread.sleep(200);

            // 测试GET
            User user = userFeignClientWithBulkhead.getUserById(1);
            result.append("GET操作: ").append(user.getUserName()).append("\n");

            // 测试POST
            User newUser = new User();
            newUser.setUserName("隔离器测试用户");
            newUser.setPassWord("123456");
            newUser.setAge(25);
            String createResult = userFeignClientWithBulkhead.createUser(newUser);
            result.append("POST操作: ").append(createResult).append("\n");

            // 测试PUT
            User updateUser = new User();
            updateUser.setUserName("隔离器更新用户");
            updateUser.setPassWord("654321");
            updateUser.setAge(30);
            String updateResult = userFeignClientWithBulkhead.updateUser(1, updateUser);
            result.append("PUT操作: ").append(updateResult).append("\n");

            // 测试DELETE
            String deleteResult = userFeignClientWithBulkhead.deleteUser(1);
            result.append("DELETE操作: ").append(deleteResult).append("\n");

            // 测试GET ALL
            Map<Integer, User> allUsers = userFeignClientWithBulkhead.getAllUsers();
            result.append("GET ALL操作: 获取到").append(allUsers.size()).append("个用户\n");

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("FeignClient隔离器CRUD操作异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient隔离器CRUD操作的降级方法
     */
    public String feignBulkheadCrudFallback(Exception ex) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return String.format("【FeignClient隔离器保护CRUD降级】%s 所有CRUD操作暂时不可用或超出并发限制，错误: %s。" +
                "所有用户相关操作均已降级保护。", timestamp, ex.getMessage());
    }

    /**
     * 批量测试FeignClient隔离器保护的并发效果
     */
    @GetMapping("/feign/bulkhead/concurrent/test/{threadCount}")
    public String batchTestFeignWithBulkhead(@PathVariable("threadCount") Integer threadCount) {
        if (threadCount > 30) {
            return "为了系统安全，FeignClient隔离器测试线程数量限制在30以内";
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        // 创建多个并发请求
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return testFeignWithBulkhead(1);
                } catch (Exception e) {
                    return "FeignClient并发请求异常: " + e.getMessage();
                }
            });
            futures.add(future);
        }
        
        // 等待所有请求完成
        List<String> results = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("FeignClient Future异常: " + e.getMessage());
            }
        }
        
        // 统计结果
        int successCount = 0;
        int feignFallbackCount = 0;
        int bulkheadFallbackCount = 0;
        
        for (String result : results) {
            if (result.contains("成功调用")) {
                successCount++;
            } else if (result.contains("FeignClient内置降级")) {
                feignFallbackCount++;
            } else if (result.contains("FeignClient + 隔离器双重保护降级")) {
                bulkheadFallbackCount++;
            }
        }
        
        String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("【FeignClient隔离器并发测试】开始: %s，结束: %s\n", startTime, endTime));
        summary.append(String.format("总请求数: %d，成功处理: %d，FeignClient降级: %d，隔离器降级: %d\n", 
                threadCount, successCount, feignFallbackCount, bulkheadFallbackCount));
        summary.append("双重保护配置：FeignClient内置fallback + 隔离器（最大并发数10，最大等待时间20ms）\n\n");
        
        summary.append("=== 详细结果 ===\n");
        for (int i = 0; i < Math.min(results.size(), 10); i++) { // 只显示前10个结果避免太长
            summary.append(String.format("线程%02d: %s\n", i+1, results.get(i)));
        }
        
        if (results.size() > 10) {
            summary.append(String.format("... 还有%d个结果未显示\n", results.size() - 10));
        }
        
        return summary.toString();
    }

    /**
     * 高频快速测试FeignClient隔离器性能
     */
    @GetMapping("/feign/bulkhead/performance/test/{requestCount}")
    public String testFeignBulkheadPerformance(@PathVariable("requestCount") Integer requestCount) {
        if (requestCount > 50) {
            return "为了系统安全，FeignClient隔离器性能测试请求数量限制在50以内";
        }

        long startTime = System.currentTimeMillis();
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // 快速发送所有请求
        for (int i = 0; i < requestCount; i++) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return testFeignFastWithBulkhead(1);
            });
            futures.add(future);
        }
        
        // 收集结果
        List<String> results = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("FeignClient性能测试异常: " + e.getMessage());
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 统计分析
        int successCount = 0;
        int feignFallbackCount = 0;
        int bulkheadFallbackCount = 0;
        
        for (String result : results) {
            if (result.contains("快速调用成功")) {
                successCount++;
            } else if (result.contains("FeignClient内置降级")) {
                feignFallbackCount++;
            } else if (result.contains("FeignClient + 隔离器双重保护降级")) {
                bulkheadFallbackCount++;
            }
        }
        
        StringBuilder report = new StringBuilder();
        report.append("【FeignClient隔离器性能测试报告】\n");
        report.append(String.format("测试参数：请求数量=%d，处理时间=50ms/请求\n", requestCount));
        report.append(String.format("配置参数：最大并发数=10，最大等待时间=20ms\n"));
        report.append(String.format("测试结果：总耗时=%dms，成功=%d，FeignClient降级=%d，隔离器降级=%d\n", 
                totalTime, successCount, feignFallbackCount, bulkheadFallbackCount));
        report.append(String.format("性能指标：成功率=%.2f%%，吞吐量=%.2f请求/秒\n", 
                (double)successCount/requestCount*100, (double)successCount*1000/totalTime));
        
        if (feignFallbackCount > 0 || bulkheadFallbackCount > 0) {
            report.append("\n【分析】FeignClient提供双重保护：\n");
            report.append("1. 隔离器保护：限制并发数，超出时快速拒绝\n");
            report.append("2. FeignClient内置降级：服务不可用时的备用响应\n");
            report.append("这种设计确保了在高并发和服务异常情况下的系统稳定性。\n");
        }
        
        return report.toString();
    }

    // ================== 限流器演示方法 ==================
    
    private final AtomicInteger rateLimiterRequestCounter = new AtomicInteger(0);

    /**
     * 使用限流器进行流量控制的服务调用
     * 配置：每2秒最多处理5个请求
     */
    @GetMapping("/ratelimiter/test/{userId}")
    @RateLimiter(name = "userServiceRateLimiter", fallbackMethod = "rateLimiterFallback")
    public String testWithRateLimiter(@PathVariable("userId") Integer userId) {
        int requestId = rateLimiterRequestCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 模拟业务处理时间（200-500ms）
            int processingTime = 200 + (int)(Math.random() * 300);
            Thread.sleep(processingTime);
            
            User user = userFeignClient.getUserById(userId);
            return String.format("【限流器保护】请求#%d 在 %s 成功处理，耗时%dms，获取用户: %s (ID: %d)", 
                    requestId, timestamp, processingTime, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            return String.format("【限流器保护】请求#%d 在 %s 处理失败: %s", 
                    requestId, timestamp, e.getMessage());
        }
    }

    /**
     * 限流器的降级方法
     */
    public String rateLimiterFallback(Integer userId, Exception ex) {
        int requestId = rateLimiterRequestCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        return String.format("【限流器降级】请求#%d 在 %s 被限流拒绝，用户ID: %d，原因: %s。" +
                "配置：每2秒最多处理5个请求，当前已达到限制", 
                requestId, timestamp, userId, ex.getMessage());
    }

    /**
     * 快速限流器测试（用于观察限流效果）
     */
    @GetMapping("/ratelimiter/fast/{userId}")
    @RateLimiter(name = "userServiceRateLimiter", fallbackMethod = "rateLimiterFallback")
    public String testFastWithRateLimiter(@PathVariable("userId") Integer userId) {
        int requestId = rateLimiterRequestCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 快速处理（50ms）
            Thread.sleep(50);
            
            User user = userFeignClient.getUserById(userId);
            return String.format("【限流器保护-快速】请求#%d 在 %s 快速处理完成，获取用户: %s (ID: %d)", 
                    requestId, timestamp, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            return String.format("【限流器保护-快速】请求#%d 在 %s 处理失败: %s", 
                    requestId, timestamp, e.getMessage());
        }
    }

    /**
     * 批量测试限流器效果
     */
    @GetMapping("/ratelimiter/batch/test/{requestCount}")
    public String batchTestWithRateLimiter(@PathVariable("requestCount") Integer requestCount) {
        if (requestCount > 20) {
            return "为了演示效果，限流器测试请求数量限制在20以内";
        }

        List<String> results = new ArrayList<>();
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        // 快速连续发送请求（模拟高频访问）
        for (int i = 0; i < requestCount; i++) {
            try {
                String result = testFastWithRateLimiter(1);
                results.add(result);
                
                // 小间隔发送请求
                Thread.sleep(100);
            } catch (Exception e) {
                results.add("批量请求异常: " + e.getMessage());
            }
        }
        
        // 统计结果
        int successCount = 0;
        int rateLimitedCount = 0;
        
        for (String result : results) {
            if (result.contains("快速处理完成")) {
                successCount++;
            } else if (result.contains("限流器降级") || result.contains("被限流拒绝")) {
                rateLimitedCount++;
            }
        }
        
        String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("【限流器批量测试】开始: %s，结束: %s\n", startTime, endTime));
        summary.append(String.format("总请求数: %d，成功处理: %d，限流拒绝: %d\n", 
                requestCount, successCount, rateLimitedCount));
        summary.append("配置说明：每2秒最多处理5个请求\n\n");
        
        summary.append("=== 详细结果 ===\n");
        for (int i = 0; i < results.size(); i++) {
            summary.append(String.format("请求%02d: %s\n", i+1, results.get(i)));
        }
        
        summary.append("\n【说明】限流器按照配置的时间窗口和请求数量限制访问频率，" +
                "超出限制的请求将被立即拒绝，这是流量治理的正常表现。");
        
        return summary.toString();
    }

    /**
     * 分时段测试限流器恢复效果
     */
    @GetMapping("/ratelimiter/recovery/test")
    public String testRateLimiterRecovery() {
        StringBuilder report = new StringBuilder();
        report.append("【限流器恢复测试】\n");
        report.append("测试场景：快速发送超过限制的请求，然后等待刷新周期，观察恢复效果\n\n");
        
        // 第一阶段：快速发送8个请求（超过5个限制）
        report.append("=== 第一阶段：快速发送8个请求 ===\n");
        int successPhase1 = 0;
        int rejectedPhase1 = 0;
        
        for (int i = 1; i <= 8; i++) {
            try {
                String result = testFastWithRateLimiter(1);
                if (result.contains("快速处理完成")) {
                    successPhase1++;
                    report.append(String.format("请求%d: 成功\n", i));
                } else {
                    rejectedPhase1++;
                    report.append(String.format("请求%d: 被限流\n", i));
                }
                Thread.sleep(50); // 很短的间隔
            } catch (Exception e) {
                rejectedPhase1++;
                report.append(String.format("请求%d: 异常 - %s\n", i, e.getMessage()));
            }
        }
        
        report.append(String.format("第一阶段结果：成功%d个，被拒绝%d个\n\n", successPhase1, rejectedPhase1));
        
        // 等待刷新周期
        report.append("=== 等待刷新周期（2秒）===\n");
        try {
            Thread.sleep(2100); // 等待2.1秒，确保刷新周期过去
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 第二阶段：再次发送5个请求测试恢复
        report.append("=== 第二阶段：等待后再发送5个请求 ===\n");
        int successPhase2 = 0;
        int rejectedPhase2 = 0;
        
        for (int i = 1; i <= 5; i++) {
            try {
                String result = testFastWithRateLimiter(1);
                if (result.contains("快速处理完成")) {
                    successPhase2++;
                    report.append(String.format("请求%d: 成功\n", i));
                } else {
                    rejectedPhase2++;
                    report.append(String.format("请求%d: 被限流\n", i));
                }
                Thread.sleep(50);
            } catch (Exception e) {
                rejectedPhase2++;
                report.append(String.format("请求%d: 异常 - %s\n", i, e.getMessage()));
            }
        }
        
        report.append(String.format("第二阶段结果：成功%d个，被拒绝%d个\n\n", successPhase2, rejectedPhase2));
        
        // 分析
        report.append("=== 分析结果 ===\n");
        report.append("预期行为：第一阶段前5个请求成功，后2个被拒绝；第二阶段所有请求都应该成功\n");
        boolean phase1Expected = (successPhase1 <= 5 && rejectedPhase1 >= 2);
        boolean phase2Expected = (successPhase2 >= 4); // 允许一些容错
        
        if (phase1Expected && phase2Expected) {
            report.append("✅ FeignClient限流器工作正常：正确限制了高频请求，并在刷新周期后正确恢复\n");
        } else {
            report.append("⚠️ 结果与预期有差异，可能受到其他并发请求或网络延迟影响\n");
        }
        
        report.append("\n【特点】FeignClient + 限流器提供了既有流量控制又有服务容错的完整解决方案");
        
        return report.toString();
    }

    /**
     * 限流器状态监控
     */
    @GetMapping("/ratelimiter/status")
    public String getRateLimiterStatus() {
        return "【限流器状态监控】\n" +
               "实例名称: userServiceRateLimiter\n" +
               "刷新周期: 2秒\n" +
               "周期内最大请求数: 5个\n" +
               "超时时间: 0ms（立即拒绝）\n" +
               "工作原理: 采用令牌桶算法，每2秒刷新一次令牌，最多存储5个令牌\n" +
               "限流策略: 当请求到达时消耗1个令牌，令牌不足时立即拒绝请求\n" +
               "适用场景: 保护系统免受突发流量冲击，确保服务稳定性和公平性";
    }

    /**
     * 简单的限流器测试（用于快速验证）
     */
    @GetMapping("/ratelimiter/simple/test")
    @RateLimiter(name = "userServiceRateLimiter", fallbackMethod = "simpleFallback")
    public String simpleRateLimiterTest() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return String.format("【简单限流测试】%s 请求处理成功！配置：每2秒最多5个请求", timestamp);
    }

    /**
     * 简单测试的降级方法
     */
    public String simpleFallback(Exception ex) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return String.format("【简单限流测试-降级】%s 请求被限流拒绝！当前流量过大，请稍后再试", timestamp);
    }

    // ================== OpenFeign限流器演示方法 ==================
    
    // 注入使用限流器保护的FeignClient
    @Resource
    private UserFeignClientWithRateLimit userFeignClientWithRateLimit;

    /**
     * 使用限流器保护的FeignClient进行服务调用
     * 双重保护：@RateLimiter注解 + FeignClient内置fallback
     */
    @GetMapping("/feign/ratelimiter/getUserById/{userId}")
    @RateLimiter(name = "userServiceRateLimiter", fallbackMethod = "feignRateLimiterFallback")
    public String testFeignWithRateLimit(@PathVariable("userId") Integer userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            // 模拟业务处理时间（100-300ms）
            int processingTime = 100 + (int)(Math.random() * 200);
            Thread.sleep(processingTime);
            
            User user = userFeignClientWithRateLimit.getUserById(userId);
            
            // 判断是否为降级响应
            if (user.getUserId() == -888) {
                return String.format("【FeignClient限流器保护】%s 触发了FeignClient内置降级，时间：%s", 
                        user.getUserName(), timestamp);
            }
            
            return String.format("【FeignClient限流器保护】%s 成功调用，耗时%dms，获取用户: %s (ID: %d)", 
                    timestamp, processingTime, user.getUserName(), user.getUserId());
        } catch (Exception e) {
            throw new RuntimeException("FeignClient限流器保护调用异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient + 限流器的外层降级方法
     */
    public String feignRateLimiterFallback(Integer userId, Exception ex) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return String.format("【FeignClient + 限流器双重保护降级】%s 服务暂时不可用或超出流量限制，用户ID: %d，错误: %s。" +
                "配置：FeignClient内置fallback + 限流器（每2秒最多5个请求）", 
                timestamp, userId, ex.getMessage());
    }

    /**
     * 测试FeignClient限流器保护的完整CRUD操作
     */
    @GetMapping("/feign/ratelimiter/crud/test")
    @RateLimiter(name = "userServiceRateLimiter", fallbackMethod = "feignRateLimiterCrudFallback")
    public String testFeignRateLimitCrud() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        try {
            StringBuilder result = new StringBuilder();
            result.append(String.format("【FeignClient限流器保护CRUD测试】开始时间: %s\n\n", timestamp));

            // 模拟处理时间
            Thread.sleep(100);

            // 测试GET
            User user = userFeignClientWithRateLimit.getUserById(1);
            result.append("GET操作: ").append(user.getUserName()).append("\n");

            // 测试POST
            User newUser = new User();
            newUser.setUserName("限流器测试用户");
            newUser.setPassWord("123456");
            newUser.setAge(25);
            String createResult = userFeignClientWithRateLimit.createUser(newUser);
            result.append("POST操作: ").append(createResult).append("\n");

            // 测试PUT
            User updateUser = new User();
            updateUser.setUserName("限流器更新用户");
            updateUser.setPassWord("654321");
            updateUser.setAge(30);
            String updateResult = userFeignClientWithRateLimit.updateUser(1, updateUser);
            result.append("PUT操作: ").append(updateResult).append("\n");

            // 测试DELETE
            String deleteResult = userFeignClientWithRateLimit.deleteUser(1);
            result.append("DELETE操作: ").append(deleteResult).append("\n");

            // 测试GET ALL
            Map<Integer, User> allUsers = userFeignClientWithRateLimit.getAllUsers();
            result.append("GET ALL操作: 获取到").append(allUsers.size()).append("个用户\n");

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("FeignClient限流器CRUD操作异常: " + e.getMessage());
        }
    }

    /**
     * FeignClient限流器CRUD操作的降级方法
     */
    public String feignRateLimiterCrudFallback(Exception ex) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return String.format("【FeignClient限流器保护CRUD降级】%s 所有CRUD操作暂时不可用或超出流量限制，错误: %s。" +
                "所有用户相关操作均已降级保护。", timestamp, ex.getMessage());
    }

    /**
     * 批量测试FeignClient限流器保护的流量控制效果
     */
    @GetMapping("/feign/ratelimiter/batch/test/{requestCount}")
    public String batchTestFeignWithRateLimit(@PathVariable("requestCount") Integer requestCount) {
        if (requestCount > 15) {
            return "为了演示效果，FeignClient限流器测试请求数量限制在15以内";
        }

        List<String> results = new ArrayList<>();
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        // 快速连续发送请求测试限流效果
        for (int i = 0; i < requestCount; i++) {
            try {
                String result = testFeignWithRateLimit(1);
                results.add(result);
                
                // 很短的间隔发送请求
                Thread.sleep(50);
            } catch (Exception e) {
                results.add("FeignClient批量请求异常: " + e.getMessage());
            }
        }
        
        // 统计结果
        int successCount = 0;
        int feignFallbackCount = 0;
        int rateLimiterFallbackCount = 0;
        
        for (String result : results) {
            if (result.contains("成功调用")) {
                successCount++;
            } else if (result.contains("FeignClient内置降级")) {
                feignFallbackCount++;
            } else if (result.contains("FeignClient + 限流器双重保护降级")) {
                rateLimiterFallbackCount++;
            }
        }
        
        String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("【FeignClient限流器批量测试】开始: %s，结束: %s\n", startTime, endTime));
        summary.append(String.format("总请求数: %d，成功处理: %d，FeignClient降级: %d，限流器降级: %d\n", 
                requestCount, successCount, feignFallbackCount, rateLimiterFallbackCount));
        summary.append("双重保护配置：FeignClient内置fallback + 限流器（每2秒最多5个请求）\n\n");
        
        summary.append("=== 详细结果 ===\n");
        for (int i = 0; i < results.size(); i++) {
            summary.append(String.format("请求%02d: %s\n", i+1, results.get(i)));
        }
        
        summary.append("\n【说明】FeignClient提供双重保护：\n");
        summary.append("1. 限流器保护：控制请求频率，超出时快速拒绝\n");
        summary.append("2. FeignClient内置降级：服务不可用时的备用响应\n");
        summary.append("预期效果：前5个请求成功，后续请求被限流器拒绝");
        
        return summary.toString();
    }

    /**
     * 测试FeignClient限流器在时间窗口恢复后的表现
     */
    @GetMapping("/feign/ratelimiter/recovery/test")
    public String testFeignRateLimitRecovery() {
        StringBuilder report = new StringBuilder();
        report.append("【FeignClient限流器恢复测试】\n");
        report.append("测试场景：快速发送超过限制的请求，然后等待刷新周期，观察恢复效果\n\n");
        
        // 第一阶段：快速发送7个请求（超过5个限制）
        report.append("=== 第一阶段：快速发送7个请求 ===\n");
        int successPhase1 = 0;
        int rejectedPhase1 = 0;
        
        for (int i = 1; i <= 7; i++) {
            try {
                String result = testFeignWithRateLimit(1);
                if (result.contains("成功调用")) {
                    successPhase1++;
                    report.append(String.format("请求%d: 成功\n", i));
                } else {
                    rejectedPhase1++;
                    report.append(String.format("请求%d: 被限流或降级\n", i));
                }
                Thread.sleep(30); // 很短的间隔
            } catch (Exception e) {
                rejectedPhase1++;
                report.append(String.format("请求%d: 异常 - %s\n", i, e.getMessage()));
            }
        }
        
        report.append(String.format("第一阶段结果：成功%d个，被拒绝%d个\n\n", successPhase1, rejectedPhase1));
        
        // 等待刷新周期
        report.append("=== 等待刷新周期（2秒）===\n");
        try {
            Thread.sleep(2100); // 等待2.1秒，确保刷新周期过去
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 第二阶段：再次发送5个请求测试恢复
        report.append("=== 第二阶段：等待后再发送5个请求 ===\n");
        int successPhase2 = 0;
        int rejectedPhase2 = 0;
        
        for (int i = 1; i <= 5; i++) {
            try {
                String result = testFeignWithRateLimit(1);
                if (result.contains("成功调用")) {
                    successPhase2++;
                    report.append(String.format("请求%d: 成功\n", i));
                } else {
                    rejectedPhase2++;
                    report.append(String.format("请求%d: 被限流或降级\n", i));
                }
                Thread.sleep(30);
            } catch (Exception e) {
                rejectedPhase2++;
                report.append(String.format("请求%d: 异常 - %s\n", i, e.getMessage()));
            }
        }
        
        report.append(String.format("第二阶段结果：成功%d个，被拒绝%d个\n\n", successPhase2, rejectedPhase2));
        
        // 分析
        report.append("=== 分析结果 ===\n");
        report.append("预期行为：第一阶段前5个请求成功，后2个被拒绝；第二阶段所有请求都应该成功\n");
        boolean phase1Expected = (successPhase1 <= 5 && rejectedPhase1 >= 2);
        boolean phase2Expected = (successPhase2 >= 4); // 允许一些容错
        
        if (phase1Expected && phase2Expected) {
            report.append("✅ FeignClient限流器工作正常：正确限制了高频请求，并在刷新周期后正确恢复\n");
        } else {
            report.append("⚠️ 结果与预期有差异，可能受到其他并发请求或网络延迟影响\n");
        }
        
        report.append("\n【特点】FeignClient + 限流器提供了既有流量控制又有服务容错的完整解决方案");
        
        return report.toString();
    }
}
