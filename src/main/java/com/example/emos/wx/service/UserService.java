package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.ContactList;
import com.example.emos.wx.db.pojo.TbEmployee;
import com.example.emos.wx.db.pojo.TbUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface UserService {
    String getOpenId(String code);

    public int registerUser(String registerCode, String code, String nickname, String photo, TbEmployee employee);

    public Set<String> searchUserPermission(int userId);

    public Integer login(String code);

    public TbUser searchById(int userId);

    public String searchHireDate(int userId);

    public HashMap searchUserSummary(int userId);

    public ArrayList<HashMap> searchUserAndDeptByUserName(String keyword);

    public ArrayList<HashMap> searchMembers(List param);

    List<ContactList>  searchUserListGroupByDept();


}
