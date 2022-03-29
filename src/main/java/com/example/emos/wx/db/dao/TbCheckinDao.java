package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbCheckinDao {
   public Integer haveCheckin(HashMap map);

   public void insert(TbCheckin checkin);

   public ArrayList<HashMap> searchWeekCheckin(HashMap map);

   public HashMap searchTodayCheckin(int userId);

   public long searchCheckinDays(int userId);

    int searchHighRiskCheckin(int userId);

   int searchMiddleRiskCheckin(int userId);

   int searchRiskCount(HashMap map);
}