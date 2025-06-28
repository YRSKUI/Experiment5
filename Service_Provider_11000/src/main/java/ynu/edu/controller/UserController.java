package ynu.edu.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import ynu.edu.entity.User;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RefreshScope
public class UserController {


    private static Map<Integer, User> userDatabase = new HashMap<>();

    static {

        userDatabase.put(1, new User(1, "张三 from 11000", "123456", 25));
        userDatabase.put(2, new User(2, "李四 from 11000", "654321", 30));
    }

    // GET方法 - 根据ID获取用户
    @GetMapping("/getUserById/{userId}")
    public User getUserById(@PathVariable("userId") Integer userId) {
        User user = userDatabase.get(userId);
        if (user != null) {
            return user;
        } else {
            // 如果用户不存在，返回默认用户
            User defaultUser = new User();
            defaultUser.setUserId(userId);
            defaultUser.setUserName("默认用户 from 11000");
            defaultUser.setPassWord("123456");
            return defaultUser;
        }
    }

    // POST方法 - 创建新用户
    @PostMapping("/createUser")
    public String createUser(@RequestBody User user) {
        if (user.getUserId() == null) {
            return "创建用户失败：用户ID不能为空";
        }
        if (userDatabase.containsKey(user.getUserId())) {
            return "创建用户失败：用户ID " + user.getUserId() + " 已存在";
        }
        userDatabase.put(user.getUserId(), user);
        return "创建用户成功：用户ID " + user.getUserId() + "，用户名：" + user.getUserName();
    }

    // PUT方法 - 更新用户信息
    @PutMapping("/updateUser/{userId}")
    public String updateUser(@PathVariable("userId") Integer userId, @RequestBody User user) {
        if (userDatabase.containsKey(userId)) {
            user.setUserId(userId);
            userDatabase.put(userId, user);
            return "更新用户成功：用户ID " + userId + "，新用户名：" + user.getUserName();
        } else {
            return "更新用户失败：用户ID " + userId + " 不存在";
        }
    }

    // DELETE方法 - 删除用户
    @DeleteMapping("/deleteUser/{userId}")
    public String deleteUser(@PathVariable("userId") Integer userId) {
        if (userDatabase.containsKey(userId)) {
            User deletedUser = userDatabase.remove(userId);
            return "删除用户成功：用户ID " + userId + "，用户名：" + deletedUser.getUserName();
        } else {
            return "删除用户失败：用户ID " + userId + " 不存在";
        }
    }

    // 获取所有用户
    @GetMapping("/getAllUsers")
    public Map<Integer, User> getAllUsers() {
        return userDatabase;
    }

    // 负载均衡测试端点
    @GetMapping("/loadBalanceTest")
    public Map<String, Object> loadBalanceTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("servicePort", "11000");
        response.put("serviceName", "Provider-Service-11000");
        response.put("message", "这是来自11000端口的响应");
        response.put("timestamp", System.currentTimeMillis());
        response.put("instanceId", "provider-instance-11000");
        return response;
    }
}
