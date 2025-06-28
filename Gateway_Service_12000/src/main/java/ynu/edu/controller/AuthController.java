package ynu.edu.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 简单的登录接口，返回token
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        Map<String, Object> result = new HashMap<>();
        
        // 简单的用户验证
        if ("admin".equals(username) && "123456".equals(password)) {
            result.put("success", true);
            result.put("token", "Bearer admin-token-123456789");
            result.put("message", "管理员登录成功");
            result.put("userType", "admin");
        } else if ("user".equals(username) && "123456".equals(password)) {
            result.put("success", true);
            result.put("token", "Bearer user-token-987654321");
            result.put("message", "普通用户登录成功");
            result.put("userType", "user");
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        
        return result;
    }

    /**
     * 获取当前用户信息（需要认证）
     */
    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                          @RequestHeader(value = "X-User-Name", required = false) String userName) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userName", userName);
        result.put("message", "通过全局过滤器认证");
        return result;
    }
} 