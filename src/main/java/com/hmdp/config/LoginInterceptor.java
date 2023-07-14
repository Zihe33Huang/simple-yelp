package com.hmdp.config;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author huangzihe
 * @date 2023/7/13 7:35 PM
 */
public class LoginInterceptor implements HandlerInterceptor {

    StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * This is for monolith app
     */
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 1. Get Session
//        HttpSession session = request.getSession();
//        // 2. get user info
//        Object user = session.getAttribute("user");
//        if (user == null) {
//            // User not existing, return 401 status code
//            response.setStatus(401);
//            return false;
//        }
//        // 3. Save user into ThreadLocal
//        UserHolder.saveUser((UserDTO) user);
//        return true;
//    }

    /**
     * This is for distributed system
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. Get Token
        String token = request.getHeader("authorization");
        // 2. get user info
        String userInfoJson = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_USER_KEY + token);

        if (StringUtils.isEmpty(userInfoJson)) {
            // User not login, return 401 status code
            response.setStatus(401);
            return false;
        }


        UserDTO user = JSONUtil.toBean(userInfoJson, UserDTO.class);
        // 3. Save user into ThreadLocal
        UserHolder.saveUser((UserDTO) user);
        return true;
    }

}
