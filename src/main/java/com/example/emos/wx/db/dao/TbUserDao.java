package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.ContactList;
import com.example.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Mapper
public interface TbUserDao {
    public boolean haveRootUser();

    public int insert(TbUser user);

    public Integer searchIdByOpenId(String openId);

    public Set<String> searchUserPermission(int userId);

    TbUser searchById(int userId);

    public HashMap searchNameAndDept(int userId);

    public String searchHireDate(int userId);

    public HashMap searchUserSummary(int userId);

    public ArrayList<HashMap> searchUserAndDeptByUserName(String keyword);

    public ArrayList<HashMap> searchMembers(List param);

    public HashMap searchUserInfo(int userId);

    public Integer searchDeptManagerIdByUserId(int userId);

    public Integer searchDeptManagerIdByDeptId(int deptId);

    public int searchGmId();

    List<ContactList> searchUserListGroupByDept();
}