package com.smartshuttle.system.service.impl;

import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import com.smartshuttle.common.utils.JwtUtils;
import com.smartshuttle.system.dto.LoginDTO;
import com.smartshuttle.system.entity.SysUser;
import com.smartshuttle.system.mapper.SysUserMapper;
import com.smartshuttle.system.service.AuthService;
import com.smartshuttle.system.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final SysUserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final long TOKEN_EXPIRATION = 7200L; // 2小时
    
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 查询用户
        SysUser user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw BusinessException.of(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        
        // 2. 验证状态
        if (user.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.USER_DISABLED, "用户已被禁用");
        }
        
        // 3. 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {//该方法会取数据库中储存的密码，取其盐值，对输入密码进行加密后验证是否匹配
            System.out.println(passwordEncoder.encode(loginDTO.getPassword()));
            throw BusinessException.of(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        
        // 4. 查询角色和权限
        List<String> roles = userMapper.selectRolesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionsByUserId(user.getId());
        
        // 5. 生成Token
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());
        
        // 6. 存储RefreshToken到Redis
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                7, TimeUnit.DAYS
        );
        
        // 7. 构建返回结果
        LoginVO.UserInfoVO userInfo = LoginVO.UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(permissions)
                .build();
        
        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(TOKEN_EXPIRATION)
                .userInfo(userInfo)
                .build();
    }
    
    @Override
    public void logout(String token) {
        // 将Token加入黑名单
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        try {
            Long userId = jwtUtils.getUserId(token);
            // Token加入黑名单，有效期设为Token剩余有效期
            redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    TOKEN_EXPIRATION, TimeUnit.SECONDS
            );
            // 删除RefreshToken
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
            log.info("用户 {} 登出成功", userId);
        } catch (Exception e) {
            log.warn("登出处理异常: {}", e.getMessage());
        }
    }
    
    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 1. 验证RefreshToken
        Long userId;
        try {
            userId = jwtUtils.getUserId(refreshToken);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID, "无效的刷新令牌");
        }
        
        // 2. 检查Redis中是否存在
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID, "刷新令牌已失效");
        }
        
        // 3. 查询用户信息
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.USER_DISABLED, "用户不存在或已禁用");
        }
        
        // 4. 重新生成Token
        List<String> roles = userMapper.selectRolesByUserId(userId);
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);
        
        String newAccessToken = jwtUtils.generateToken(userId, user.getUsername(), roles);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId);
        
        // 5. 更新Redis中的RefreshToken
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                7, TimeUnit.DAYS
        );
        
        // 6. 构建返回结果
        LoginVO.UserInfoVO userInfo = LoginVO.UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(permissions)
                .build();
        
        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(TOKEN_EXPIRATION)
                .userInfo(userInfo)
                .build();
    }
    
    @Override
    public LoginVO.UserInfoVO getCurrentUserInfo() {
        // 从SecurityContext获取当前用户（简化实现）
        // 实际项目中应该从SecurityContextHolder获取
        return null;
    }
}
