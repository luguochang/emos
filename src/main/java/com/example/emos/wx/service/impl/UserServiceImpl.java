package com.example.emos.wx.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbDeptDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.ContactList;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbEmployee;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;




import java.util.*;

@Service
@Scope("prototype")
@Slf4j
public class UserServiceImpl implements UserService {


    private static final String ROOT_REGISTER_CODE = "000000";

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private MessageTask messageTask;

    @Autowired
    private TbDeptDao deptDao;

    @Override
    public String getOpenId(String code) {
        String url =  "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap<>();
        map.put("appid",appId);
        map.put("secret",appSecret);
        map.put("js_code",code);
        map.put("grant_type","authorization_code");

        String response = HttpUtil.post(url, map);
        JSONObject json = JSONUtil.parseObj(response);
        String openid = json.getStr("openid");
        if (openid==null || openid.length() == 0){
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openid;

    }

    @Override
    public int registerUser(String registerCode, String code, String nickName, String photo, TbEmployee employee) {

        String openId = getOpenId(code);
        if (registerCode.equals(ROOT_REGISTER_CODE)) {

            boolean hasRootUser = userDao.haveRootUser();
            if (!hasRootUser) {
                //注册管理员
                TbUser user = getUserEntity(true, openId, code, nickName, photo,employee);

                userDao.insert(user);

                int id = userDao.searchIdByOpenId(user.getOpenId());
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
                entity.setSenderName("系统消息");
                entity.setUuid(IdUtil.simpleUUID());
                entity.setMsg("欢迎您注册成为超级管理员，请及时更新您的员工个人信息");
                entity.setSendTime(new Date());

                messageTask.sendAsync(id + "", entity);

            } else {
                throw new EmosException("创建超级管理员失败");
            }
        } else {
            TbUser user = getUserEntity(false, openId, code, nickName, photo,employee);

            userDao.insert(user);
            int userId = userDao.searchIdByOpenId(user.getOpenId());

            MessageEntity entity = new MessageEntity();
            entity.setSenderId(0);
            entity.setSenderName("系统消息");
            entity.setUuid(IdUtil.simpleUUID());
            entity.setMsg("欢迎来到emos，请及时更新您的员工个人信息");
            entity.setSendTime(new Date());
            messageTask.sendAsync(userId + "", entity);


        }
        return userDao.searchIdByOpenId(openId);
    }

    private TbUser getUserEntity(boolean isRootUser, String openId, String code, String nickName, String photo, TbEmployee employee) {

        TbUser user = new TbUser();
        user.setRoot(isRootUser);
        user.setOpenId(openId);
        user.setNickname(nickName);
        user.setPhoto(photo);
        user.setCreateTime(new Date());
        user.setStatus((byte) 1);
        user.setSex(employee.getSex());
        user.setName(employee.getName());
        user.setTel(employee.getTel());
        user.setEmail(employee.getEmail());
        user.setDeptId(employee.getDeptId());
        user.setHiredate(employee.getHiredate());
        user.setRole(employee.getRole());
        return user;
    }


    @Override
    public Set<String> searchUserPermission(int userId) {
        Set<String> permission = userDao.searchUserPermission(userId);
        return permission;
    }

    @Override
    public Integer login(String code) {

        String openId = getOpenId(code);
        Integer id = userDao.searchIdByOpenId(openId);
        if (openId==null){
            throw new EmosException("账户不存在");
        }
        messageTask.receiveAsync(id+"");
        return id;

    }

    @Override
    public TbUser searchById(int userId) {
        return userDao.searchById(userId);
    }

    @Override
    public String searchHireDate(int userId) {
        String hireDate = userDao.searchHireDate(userId);
        return hireDate;
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        return userDao.searchUserSummary(userId);
    }

    @Override
    public ArrayList<HashMap> searchUserAndDeptByUserName(String keyword){
        ArrayList<HashMap> list_1 = deptDao.searchDeptMembersCount(keyword);
        ArrayList<HashMap> list_2 = userDao.searchUserAndDeptByUserName(keyword);

        for (HashMap map_1 : list_1){
            long dept_id = (long) map_1.get("id");
            ArrayList members = new ArrayList();
            for (HashMap map_2 : list_2){
                long deptId = (long) map_2.get("deptId");
                if (deptId==dept_id){
                    members.add(map_2);
                }
            }
            map_1.put("members",members);
        }
        return list_1;
    }

    @Override
    public ArrayList<HashMap> searchMembers(List param) {
        return userDao.searchMembers(param);
    }


    @Override
    public List<ContactList> searchUserListGroupByDept() {
        return userDao.searchUserListGroupByDept();
    }


}
