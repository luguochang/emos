package com.example.emos.wx.config.shiro;


import org.apache.shiro.authc.AuthenticationToken;

/**
 * 生成的token不能直接使用，要封装到shiro框架中，实现AuthenticationToken
 */
public class OAuth2Token implements AuthenticationToken {
    private String token;

    public OAuth2Token(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
