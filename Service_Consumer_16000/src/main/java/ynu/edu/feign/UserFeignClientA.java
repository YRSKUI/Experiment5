package ynu.edu.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ynu.edu.entity.User;
import ynu.edu.feign.fallback.UserFeignClientAFallback;

import java.util.Map;

/**
 * FeignClient接口A - 使用断路器实例A进行容错保护
 * 配置: 失败率阈值30%，时间窗口10秒，最小请求数5
 */
@FeignClient(name = "provider-service", 
             fallback = UserFeignClientAFallback.class,
             contextId = "userFeignClientA")
public interface UserFeignClientA {

    /**
     * 调用服务提供者的GET方法 - 根据ID获取用户
     */
    @GetMapping("/user/getUserById/{userId}")
    User getUserById(@PathVariable("userId") Integer userId);

    /**
     * 调用服务提供者的POST方法 - 创建新用户
     */
    @PostMapping("/user/createUser")
    String createUser(@RequestBody User user);

    /**
     * 调用服务提供者的PUT方法 - 更新用户信息
     */
    @PutMapping("/user/updateUser/{userId}")
    String updateUser(@PathVariable("userId") Integer userId, @RequestBody User user);

    /**
     * 调用服务提供者的DELETE方法 - 删除用户
     */
    @DeleteMapping("/user/deleteUser/{userId}")
    String deleteUser(@PathVariable("userId") Integer userId);

    /**
     * 调用服务提供者的GET方法 - 获取所有用户
     */
    @GetMapping("/user/getAllUsers")
    Map<Integer, User> getAllUsers();
} 