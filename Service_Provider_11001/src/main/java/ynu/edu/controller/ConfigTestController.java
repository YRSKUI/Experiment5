package ynu.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.edu.config.DynamicConfig;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
@RefreshScope
public class ConfigTestController {
    
    @Autowired
    private DynamicConfig dynamicConfig;
    
    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("appName", dynamicConfig.getAppName());
        result.put("appVersion", dynamicConfig.getAppVersion());
        result.put("appDescription", dynamicConfig.getAppDescription());
        result.put("featureEnabled", dynamicConfig.isFeatureEnabled());
        result.put("servicePort", "11001");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
} 