package com.example.emos.wx.aop;

import com.example.emos.wx.common.utils.R;
import com.example.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TokenAop {

    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..)))")
    public void aspect(){}

    /**
     * 环绕切面： joinPoint.proceed 是方法执行前面
     * 这里是方法执行后如果 Thredlocal中有token则取出来，返回给client
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("aspect()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        R r=(R)joinPoint.proceed();
        String token = threadLocalToken.getToken();
        if (token!=null){
            r.put("token",token);
            threadLocalToken.clear();
        }
        return r;

    }
}
