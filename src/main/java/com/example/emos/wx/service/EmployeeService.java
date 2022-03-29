package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.TbEmployee;
import com.example.emos.wx.db.pojo.EmployeeList;

import java.util.HashMap;
import java.util.List;

public interface EmployeeService {
    TbEmployee searchByCode(int code);

    List<EmployeeList> searchEmployList();

    List<TbEmployee> searchContact(String name);


    TbEmployee insertEmployee(TbEmployee employee);

    List<TbEmployee> searchUnActiveEmployees();

    void  updateState(HashMap params);

    void  updateEmployee(TbEmployee employee);

    void  deleteEmployee(int code);

    TbEmployee searchEmployee(int code);
}
