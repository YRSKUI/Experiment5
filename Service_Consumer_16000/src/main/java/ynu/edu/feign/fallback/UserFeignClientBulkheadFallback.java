package ynu.edu.feign.fallback;

import org.springframework.stereotype.Component;
import ynu.edu.entity.User;
import ynu.edu.feign.UserFeignClientWithBulkhead;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用隔离器保护的FeignClient降级响应类
 * 当隔离器限制并发或服务不可用时，会调用此类中的方法作为降级响应
 */
@Component
public class UserFeignClientBulkheadFallback implements UserFeignClientWithBulkhead {

    @Override
    public User getUserById(Integer userId) {
        // 返回默认用户信息
        User fallbackUser = new User();
        fallbackUser.setUserId(-999);
        fallbackUser.setUserName("【隔离器降级】服务暂时不可用或并发限制-用户ID:" + userId + 
                " [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "]");
        fallbackUser.setPassWord("N/A");
        fallbackUser.setAge(0);
        return fallbackUser;
    }

    @Override
    public String createUser(User user) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return "【隔离器降级】用户创建服务暂时不可用或超出并发限制，请稍后重试。" +
               "尝试创建的用户: " + (user != null ? user.getUserName() : "未知") +
               "。隔离器配置：最大并发数10，最大等待时间20ms。时间：" + timestamp;
    }

    @Override
    public String updateUser(Integer userId, User user) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return "【隔离器降级】用户更新服务暂时不可用或超出并发限制，请稍后重试。" +
               "尝试更新的用户ID: " + userId +
               "，用户信息: " + (user != null ? user.getUserName() : "未知") +
               "。隔离器配置：最大并发数10，最大等待时间20ms。时间：" + timestamp;
    }

    @Override
    public String deleteUser(Integer userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return "【隔离器降级】用户删除服务暂时不可用或超出并发限制，请稍后重试。" +
               "尝试删除的用户ID: " + userId +
               "。隔离器配置：最大并发数10，最大等待时间20ms。时间：" + timestamp;
    }

    @Override
    public Map<Integer, User> getAllUsers() {
        // 返回空的用户列表和提示信息
        Map<Integer, User> fallbackUsers = new HashMap<>();
        User fallbackUser = new User();
        fallbackUser.setUserId(-999);
        fallbackUser.setUserName("【隔离器降级】获取所有用户服务暂时不可用或超出并发限制 [" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "]");
        fallbackUser.setPassWord("N/A");
        fallbackUser.setAge(0);
        fallbackUsers.put(-999, fallbackUser);
        return fallbackUsers;
    }
} 