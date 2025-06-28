package ynu.edu.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 白名单路径，这些路径不需要认证
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/gateway/info",
            "/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String path = request.getURI().getPath();
        
        // 检查是否在白名单中
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }
        
        // 获取Authorization请求头
        String token = request.getHeaders().getFirst("Authorization");
        
        if (token == null || token.isEmpty()) {
            // 没有token，返回401未授权
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            String body = "{\"error\":\"未授权访问\",\"message\":\"请求头中缺少Authorization token\",\"code\":401}";
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
        }
        
        // 简单的token验证（实际项目中应该验证JWT等）
        if (!isValidToken(token)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            String body = "{\"error\":\"无效token\",\"message\":\"Authorization token无效\",\"code\":403}";
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
        }
        
        // token验证通过，添加用户信息到请求头
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", extractUserIdFromToken(token))
                .header("X-User-Name", extractUserNameFromToken(token))
                .build();
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 验证token是否有效
     * 简单实现：以"Bearer "开头且长度大于10的为有效token
     */
    private boolean isValidToken(String token) {
        return token.startsWith("Bearer ") && token.length() > 10;
    }

    /**
     * 从token中提取用户ID
     */
    private String extractUserIdFromToken(String token) {
        // 简单实现：根据token长度生成用户ID
        return String.valueOf(token.length() % 1000);
    }

    /**
     * 从token中提取用户名
     */
    private String extractUserNameFromToken(String token) {
        // 简单实现：根据token内容生成用户名
        if (token.contains("admin")) {
            return "管理员";
        } else if (token.contains("user")) {
            return "普通用户";
        } else {
            return "访客";
        }
    }

    @Override
    public int getOrder() {
        // 设置过滤器优先级，数字越小优先级越高
        return -100;
    }
} 