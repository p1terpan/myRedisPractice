package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @author Jupiter
 * @version 1.0
 * @description 全局拦截器
 * @date 2025/9/28 22:39
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 2.基于token获取redis中的用户
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash()
                .entries(key);

        // 3.判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }

        // 5.把查询到的hash转回userdto对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6.存在保存到threadlocal，这里用tl是因为需要有一个东西把user穿过拦截器带出去
        UserHolder.saveUser(userDTO);
        // 7.刷新token有效期
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.SECONDS);
        // 放行
        return true;
    }

    // 拦截器----基于session方法
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 1.获取session
//        HttpSession session = request.getSession();
//        // 2.获取session中的用户
//        Object user = session.getAttribute("user");
//        // 3.判断用户是否存在
//        if (user == null) {
//            // 4.不存在拦截
//            response.setStatus(401); // 返回未授权
//            return false;
//        }
//        // 5.存在保存到threadlocal，这里用tl是因为需要有一个东西把user穿过拦截器带出去
//        UserHolder.saveUser((UserDTO) user);
//        return true;
//    }


//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        UserHolder.removeUser();
//    }
}
