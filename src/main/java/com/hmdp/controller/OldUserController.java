package com.hmdp.controller;


import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.BeanProperty;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/v1/user")
public class OldUserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    public final String DELIMiTER = ":";

    /**
     * Send mobile verification code
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
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

    /**
     * login
     * @param loginForm  parameter: phone_num & verification_code  or phone_num & password
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
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
        if ( !inputCode.equals(code)) {
            return Result.fail("Code verification fails");
        }
        // 3. Check if user exists
        User user = userService.query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            user = new User();
            user.setNickName("default");
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(user.getCreateTime());
            user.setId(RandomUtil.randomLong());
            user.setPhone(loginForm.getPhone());
            userService.save(user);
        }
        // 4. save only part of info
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO, UserDTO.class);
        session.setAttribute("user", userDTO);
        return Result.ok();
    }

    /**
     * logout
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        // TODO 获取当前登录的用户并返回
        return Result.fail("功能未完成");
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
