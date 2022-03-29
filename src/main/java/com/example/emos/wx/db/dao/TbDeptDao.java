package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbDeptDao {
    public ArrayList<HashMap> searchDeptMembersCount(String keyword);

    ArrayList<TbDept> searchAllDepts();

    int insertDept(String deptName);

    int deleteDept(int id);

    int updateDept(TbDept dept);


}