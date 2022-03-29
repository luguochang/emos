package com.example.emos.wx.controller;

import cn.hutool.json.JSONUtil;
import com.example.emos.wx.common.utils.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.*;
import com.example.emos.wx.db.pojo.TbEmployee;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.EmployeeService;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@Api("用户模块web接口")
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @Autowired
    private EmployeeService employeeService;



    @PostMapping("/register")
    @ApiOperation("注册用户")
    public R register(@Valid @RequestBody RegisterForm form) {

        //获取邀请码
        int code = Integer.parseInt(form.getRegisterCode());
        TbEmployee employee = employeeService.searchByCode(code);
        if (employee != null) {
            int userId = userService.registerUser(form.getRegisterCode(), form.getCode(), form.getNickname(), form.getPhoto(), employee);
            String token = jwtUtil.createToken(userId);
            Set<String> permissionSet = userService.searchUserPermission(userId);

            //将token存入到Redis中
            saveCacheToken(token, userId);

            HashMap params = new HashMap();
            params.put("code", form.getRegisterCode());
            params.put("state", 1);
            employeeService.updateState(params);
            return R.ok("用户注册成功").put("token", token).put("permission", permissionSet);
        } else {
            return R.error("注册码不对，请于管理员联系确认");
        }

    }

    private void saveCacheToken(String token, int id) {
        redisTemplate.opsForValue().set(token,id+"",cacheExpire, TimeUnit.DAYS);
    }


    @PostMapping("/checkRegisterCode")
    @ApiOperation("注册码核验")
    public R checkRegisterCode(@Valid @RequestBody RegisterCodeForm form) {

        int code = Integer.parseInt(form.getRegisterCode());
        TbEmployee employee = employeeService.searchByCode(code);
        if (employee != null) {
            return R.ok("校验成功").put("name", employee.getName());
        } else {
            return R.error("注册码不对，请于管理员联系确认");
        }

    }

    @PostMapping("/login")
    @ApiOperation("登陆系统")
    public R login(@Valid @RequestBody LoginForm form){
        System.out.println("登陆");
        String code = form.getCode();
        Integer userId = userService.login(code);
        String token = jwtUtil.createToken(userId);
        Set<String> permsSet = userService.searchUserPermission(userId);
        return R.ok("登陆成功").put("token",token).put("permission",permsSet);
    }

    @GetMapping("/searchUserSummary")
    @ApiOperation("查询用户摘要信息")
    public R searchUserSummary(@RequestHeader("token")String token){
        int userId = jwtUtil.getUserId(token);
        HashMap map = userService.searchUserSummary(userId);
        return R.ok().put("result",map);
    }

    @PostMapping("/searchUserGroupByDept")
    @ApiOperation("查询员工列表，按照部门分组排列")
    @RequiresPermissions(value = {"ROOT","EMPLOYEE:SELECT"},logical = Logical.OR)
    public R searchUserGroupAndDeptByUserName(@Valid @RequestBody SearchUserGroupAndDeptForm form){
        ArrayList<HashMap> list = userService.searchUserAndDeptByUserName(form.getKeyword());
        return R.ok().put("result",list);
    }


    @PostMapping("/searchMembers")
    @ApiOperation("查询成员")
    @RequiresPermissions(value = {"ROOT","MEETING:INSERT"},logical = Logical.OR)
    public R searchMembers(@Valid @RequestBody SearchMembersForm form){
        if (!JSONUtil.isJsonArray(form.getMembers())){
            throw new EmosException("members不是JSON数组");
        }
        List<Integer> param = JSONUtil.parseArray(form.getMembers()).toList(Integer.class);
        ArrayList<HashMap> list = userService.searchMembers(param);
        return R.ok().put("result",list);
    }


    @GetMapping("/contactList")
    @ApiOperation("通讯录")
    public R refreshMessage(@RequestHeader("token")String token){
        return R.ok().put("list",userService.searchUserListGroupByDept());
    }

    @PostMapping("/searchUserById")
    @ApiOperation("查询员工信息")
    @RequiresPermissions(value = {"ROOT", "EMPLOYEE:SELECT"}, logical = Logical.OR)
    public R searchUserById(@Valid @RequestBody SearchUserByIdForm form) {
        TbUser user = userService.searchById(form.getId());
        return R.ok().put("result", user);
    }
}
