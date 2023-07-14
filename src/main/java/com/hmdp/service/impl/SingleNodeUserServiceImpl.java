package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;


@Service
public class SingleNodeUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    public final String DELIMiTER = ":";


    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. Verify the format of phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("format of phone number is not correct");
        }
        // 2. Generate a 6-digit verification code
        String code = RandomUtil.randomString(6);
        System.out.println(code);
        // 3. store the code in session
        session.setAttribute("code", phone + DELIMiTER + code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. Verify the format of phone number
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("format of phone number is not correct");
        }
        String inputCode = loginForm.getCode();
        if (StringUtils.isEmpty(inputCode)) {
            return Result.fail("code is empty");
        }
        // 2. Verify code
        String code = (String) session.getAttribute("code");
        inputCode = phone + DELIMiTER + inputCode;
        if (!inputCode.equals(code)) {
            return Result.fail("Code verification fails");
        }
        // 3. Check if user exists
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            user = new User();
            user.setNickName("default");
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(user.getCreateTime());
            user.setId(RandomUtil.randomLong());
            user.setPhone(loginForm.getPhone());
            save(user);
        }
        // 4. save only part of info
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO, UserDTO.class);
        session.setAttribute("user", userDTO);
        return Result.ok();
    }


}
