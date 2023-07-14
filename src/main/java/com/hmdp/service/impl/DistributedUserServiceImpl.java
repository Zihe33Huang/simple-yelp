package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author huangzihe
 * @date 2023/7/13 9:53 PM
 */

@Service
@Slf4j
public class DistributedUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. Verify the format of phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("format of phone number is not correct");
        }
        // 2. Generate a 6-digit verification code
        String code = RandomUtil.randomString(6);
        System.out.println(code);
        // 3. store the code in Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, TTL, TimeUnit.MINUTES);

        log.debug("Send verification successfully, {}",code);
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
        String code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (!inputCode.equals(code)) {
            return Result.fail("Code verification fails");
        }
        // 3. Check if user exists
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            user = createNewUserByPhone(phone);
        }
        // 4. save only part of info into Redis
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO, UserDTO.class);

        String token = UUID.randomUUID().toString();

        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_USER_KEY + token, JSONUtil.toJsonStr(userDTO), TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User createNewUserByPhone(String phone) {
        User user;
        user = new User();
        user.setNickName("default");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(user.getCreateTime());
        user.setId(RandomUtil.randomLong());
        user.setPhone(phone);
        save(user);
        return user;
    }


}

