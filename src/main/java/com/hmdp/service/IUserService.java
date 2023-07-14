package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    public final String CURRENT_SERVICE = "distributedUserServiceImpl";

    public final String DELIMiTER = ":";

    public final String PHONE = "phone";

    public final String TOKEN = "token";

    public final long TTL = 5;

    Result sendCode(String phone, HttpSession session);

    Result login(@RequestBody LoginFormDTO loginForm, HttpSession session);
}
