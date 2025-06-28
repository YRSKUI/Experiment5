package ynu.edu.feign.fallback;

import org.springframework.stereotype.Component;
import ynu.edu.entity.User;
import ynu.edu.feign.UserFeignClientA;

import java.util.HashMap;
import java.util.Map;

/**
 * FeignClient A的降级响应类
 * 当断路器实例A开启时，会调用此类中的方法作为降级响应
 */
@Component
public class UserFeignClientAFallback implements UserFeignClientA {

    @Override
    public User getUserById(Integer userId) {
        // 返回默认用户信息
        User fallbackUser = new User();
        fallbackUser.setUserId(-1);
        fallbackUser.setUserName("【断路器A降级】服务暂时不可用-用户ID:" + userId);
        fallbackUser.setPassWord("N/A");
        fallbackUser.setAge(0);
        return fallbackUser;
    }

    @Override
    public String createUser(User user) {
        return "【断路器A降级】用户创建服务暂时不可用，请稍后重试。" +
               "尝试创建的用户: " + (user != null ? user.getUserName() : "未知") +
               "。断路器配置：失败率阈值30%，滑动窗口10秒，最小请求数5";
    }

    @Override
    public String updateUser(Integer userId, User user) {
        return "【断路器A降级】用户更新服务暂时不可用，请稍后重试。" +
               "尝试更新的用户ID: " + userId +
               "，用户信息: " + (user != null ? user.getUserName() : "未知") +
               "。断路器配置：失败率阈值30%，滑动窗口10秒，最小请求数5";
    }

    @Override
    public String deleteUser(Integer userId) {
        return "【断路器A降级】用户删除服务暂时不可用，请稍后重试。" +
               "尝试删除的用户ID: " + userId +
               "。断路器配置：失败率阈值30%，滑动窗口10秒，最小请求数5";
    }

    @Override
    public Map<Integer, User> getAllUsers() {
        // 返回空的用户列表和提示信息
        Map<Integer, User> fallbackUsers = new HashMap<>();
        User fallbackUser = new User();
        fallbackUser.setUserId(-1);
        fallbackUser.setUserName("【断路器A降级】获取所有用户服务暂时不可用");
        fallbackUser.setPassWord("N/A");
        fallbackUser.setAge(0);
        fallbackUsers.put(-1, fallbackUser);
        return fallbackUsers;
    }
} 