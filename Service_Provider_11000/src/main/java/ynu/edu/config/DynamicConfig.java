package ynu.edu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class DynamicConfig {
    
    @Value("${app.name:Default App Name}")
    private String appName;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${app.description:Default description}")
    private String appDescription;
    
    @Value("${app.feature.enabled:false}")
    private boolean featureEnabled;
    
    public String getAppName() {
        return appName;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public String getAppDescription() {
        return appDescription;
    }
    
    public boolean isFeatureEnabled() {
        return featureEnabled;
    }
} 