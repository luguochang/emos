package com.example.emos.wx.config.shiro;



import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class  OAuth2Realm extends AuthorizingRealm {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

    /**
     * 授权(验证权限时调用)
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {
        //应该是从authenticationInfo中保存的principal中取的
        TbUser user= (TbUser) collection.getPrimaryPrincipal();
        int userId=user.getId();
        Set<String> permsSet=userService.searchUserPermission(userId);
        SimpleAuthorizationInfo info=new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * 认证(验证登录时调用)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken=(String)token.getPrincipal();
        int userId=jwtUtil.getUserId(accessToken);
        TbUser user=userService.searchById(userId);

        if(user==null){
            throw new LockedAccountException("账号已被锁定,请联系管理员");
        }

        //保存principal,credential,realname
        SimpleAuthenticationInfo info=new SimpleAuthenticationInfo(user,accessToken,getName());
        return info;
    }
}
