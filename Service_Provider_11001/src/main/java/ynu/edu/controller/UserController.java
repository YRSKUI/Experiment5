package ynu.edu.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.edu.entity.User;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RefreshScope
public class UserController {

    @GetMapping("/getUserById/{userId}")
    public User GetUserById(@PathVariable("userId") Integer userId) {
        User user = new User();
        user.setUserId(userId);
        
        // 根据ID返回相同的用户信息，只是标识来源端口
        if (userId == 1) {
            user.setUserName("张三 from 11001");
            user.setPassWord("123456");
            user.setAge(25);
        } else if (userId == 2) {
            user.setUserName("李四 from 11001");
            user.setPassWord("654321");
            user.setAge(30);
        } else {
            user.setUserName("默认用户 from 11001");
            user.setPassWord("123456");
            user.setAge(18);
        }
        
        return user;
    }

    // 负载均衡测试端点
    @GetMapping("/loadBalanceTest")
    public Map<String, Object> loadBalanceTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("servicePort", "11001");
        response.put("serviceName", "Provider-Service-11001");
        response.put("message", "这是来自11001端口的响应");
        response.put("timestamp", System.currentTimeMillis());
        response.put("instanceId", "provider-instance-11001");
        return response;
    }
}
