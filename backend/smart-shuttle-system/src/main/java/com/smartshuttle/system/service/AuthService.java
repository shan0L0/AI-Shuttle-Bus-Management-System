package com.smartshuttle.system.service;

import com.smartshuttle.system.dto.LoginDTO;
import com.smartshuttle.system.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);
    
    /**
     * 用户登出
     */
    void logout(String token);
    
    /**
     * 刷新Token
     */
    LoginVO refreshToken(String refreshToken);
    
    /**
     * 获取当前用户信息
     */
    LoginVO.UserInfoVO getCurrentUserInfo();
}
