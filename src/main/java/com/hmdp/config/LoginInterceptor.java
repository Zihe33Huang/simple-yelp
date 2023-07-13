package com.hmdp.config;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author huangzihe
 * @date 2023/7/13 7:35 PM
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. Get Session
        HttpSession session = request.getSession();
        // 2. get user info
        Object user = session.getAttribute("user");
        if (user == null) {
            // User not existing, return 401 status code
            response.setStatus(401);
            return false;
        }
        // 3. Save user into ThreadLocal
        UserHolder.saveUser((UserDTO) user);
        return true;
    }
}
