package com.smartshuttle.system.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 登录响应VO
 */
@Data
@Builder
public class LoginVO {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    private String tokenType;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserInfoVO userInfo;
    
    /**
     * 用户信息VO
     */
    @Data
    @Builder
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String realName;
        private String avatar;
        private List<String> roles;
        private List<String> permissions;
    }
}
