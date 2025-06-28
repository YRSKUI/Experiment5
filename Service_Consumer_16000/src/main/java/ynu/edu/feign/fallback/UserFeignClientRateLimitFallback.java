package ynu.edu.feign.fallback;

import org.springframework.stereotype.Component;
import ynu.edu.entity.User;
import ynu.edu.feign.UserFeignClientWithRateLimit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用限流器保护的FeignClient降级响应类
 * 当限流器限制访问或服务不可用时，会调用此类中的方法作为降级响应
 */
@Component
public class UserFeignClientRateLimitFallback implements UserFeignClientWithRateLimit {

    @Override
    public User getUserById(Integer userId) {
        // 返回默认用户信息
        User fallbackUser = new User();
        fallbackUser.setUserId(-888);
        fallbackUser.setUserName("【限流器降级】服务暂时不可用或流量限制-用户ID:" + userId + 
                " [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "]");
        fallbackUser.setPassWord("N/A");
        fallbackUser.setAge(0);
        return fallbackUser;
    }

    @Override
    public String createUser(User user) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return "【限流器降级】用户创建服务暂时不可用或超出流量限制，请稍后重试。" +
               "尝试创建的用户: " + (user != null ? user.getUserName() : "未知") +
               "。限流器配置：每2秒最多处理5个请求。时间：" + timestamp;
    }

    @Override
    public String updateUser(Integer userId, User user) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return "【限流器降级】用户更新服务暂时不可用或超出流量限制，请稍后重试。" +
               "尝试更新的用户ID: " + userId +
               "，用户信息: " + (user != null ? user.getUserName() : "未知") +
               "。限流器配置：每2秒最多处理5个请求。时间：" + timestamp;
    }

    @Override
    public String deleteUser(Integer userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return "【限流器降级】用户删除服务暂时不可用或超出流量限制，请稍后重试。" +
               "尝试删除的用户ID: " + userId +
               "。限流器配置：每2秒最多处理5个请求。时间：" + timestamp;
    }

    @Override
    public Map<Integer, User> getAllUsers() {
        // 返回空的用户列表和提示信息
        Map<Integer, User> fallbackUsers = new HashMap<>();
        User fallbackUser = new User();
        fallbackUser.setUserId(-888);
        fallbackUser.setUserName("【限流器降级】获取所有用户服务暂时不可用或超出流量限制 [" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "]");
        fallbackUser.setPassWord("N/A");
        fallbackUser.setAge(0);
        fallbackUsers.put(-888, fallbackUser);
        return fallbackUsers;
    }
} 