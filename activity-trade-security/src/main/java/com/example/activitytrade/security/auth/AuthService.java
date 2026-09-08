package com.example.activitytrade.security.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.security.auth.dto.LoginReq;
import com.example.activitytrade.security.auth.dto.LoginResp;
import com.example.activitytrade.security.auth.dto.RegisterReq;
import com.example.activitytrade.security.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：注册 / 登录（M3）。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResp register(RegisterReq req) {
        String username = req.getUsername().trim();
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? username : req.getNickname().trim());
        user.setRole(0);
        try {
            userMapper.insert(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        return new LoginResp(jwtUtil.generate(user.getId(), user.getUsername(), user.getRole()),
                user.getId(), user.getNickname(), user.getRole());
    }

    public LoginResp login(LoginReq req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return new LoginResp(jwtUtil.generate(user.getId(), user.getUsername(), user.getRole()),
                user.getId(), user.getNickname(), user.getRole());
    }
}