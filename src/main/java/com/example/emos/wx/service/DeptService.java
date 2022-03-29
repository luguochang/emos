package com.example.emos.wx.service;



import com.example.emos.wx.db.pojo.TbDept;

import java.util.ArrayList;
import java.util.HashMap;

public interface DeptService {

    ArrayList<TbDept> searchAllDepts();
    void insertDept(String deptName);

    void deleteDept(int id);

    void updateDept(TbDept dept);


}
