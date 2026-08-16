package com.smartshuttle.system.controller;

import com.smartshuttle.common.result.Result;
import com.smartshuttle.system.dto.LoginDTO;
import com.smartshuttle.system.service.AuthService;
import com.smartshuttle.system.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {//@RequestBody LoginDTO loginDTO表示将前端POST请求的body（通常是JSON）自动反序列化为Java对象LoginDTO
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }
    
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        authService.logout(token);
        return Result.success("登出成功", null);
    }
    
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@RequestParam String refreshToken) {
        LoginVO loginVO = authService.refreshToken(refreshToken);
        return Result.success(loginVO);
    }
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userinfo")
    public Result<LoginVO.UserInfoVO> getUserInfo() {
        LoginVO.UserInfoVO userInfo = authService.getCurrentUserInfo();
        return Result.success(userInfo);
    }
}
