package icu.niunai.interceptor;

import com.alibaba.fastjson.JSON;
import icu.niunai.entity.dto.SessionUser;
import icu.niunai.utils.AESUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import icu.niunai.utils.RedisUtil;

import java.nio.charset.StandardCharsets;

import static java.util.concurrent.TimeUnit.DAYS;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (uri.contains("/login") || uri.contains("/register") || uri.contains("/error") || uri.contains("/download")) {
            //登录请求直接放行
            System.out.println("200");
            return true;
        } else {
            String userEmail = request.getHeader("email");
            String token = request.getHeader("token");

            if (token != null) {
                //操作前先解密
                byte[] key = "a=s]u-dK'U-HG=j|y-FGjY.F;F=Y@J~G".getBytes(StandardCharsets.UTF_8);
                byte[] input = AESUtil.parseHexStr2Byte(token);
                assert input != null;
                byte[] newTokenSecret = AESUtil.decrypt(key, input);

                String tokenResult = new String(newTokenSecret, StandardCharsets.UTF_8);
                //如果有页面token头，从redis中取出来比对,没有就跳到下面拦截
                boolean result = RedisUtil.hasKey(tokenResult);
                if (result) {
                    SessionUser redisSessionUser = JSON.parseObject(RedisUtil.get(tokenResult), SessionUser.class);
                    if (redisSessionUser.getEmail().equals(userEmail) && redisSessionUser.getBan() != 1) {
                        //更新token时间
                        RedisUtil.expire(tokenResult, 7, DAYS);
                        System.out.println("200");
                        return true;
                    }
                }
            }
            response.sendError(401);
            System.out.println("401");
        }
        //默认拦截
        return false;
    }
}
