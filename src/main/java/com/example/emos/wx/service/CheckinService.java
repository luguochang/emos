package com.example.emos.wx.service;

import com.example.emos.wx.controller.form.CheckinForm;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.util.ArrayList;
import java.util.HashMap;

public interface CheckinService {
    public String validCanCheckIn(int userId,String date);

    public void checkin(CheckinForm form,int userId,String path);

    public void createFaceModel(int userId,String path);

    public HashMap searchTodayCheckin(int userId);

    public long searchCheckinDays(int userId);

    public ArrayList<HashMap> searchWeekCheckin(HashMap map);

    public ArrayList<HashMap> searchMonthCheckin(HashMap param);

}
