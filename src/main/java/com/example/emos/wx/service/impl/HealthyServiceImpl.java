package com.example.emos.wx.service.impl;


import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.db.dao.TbCheckinDao;
import com.example.emos.wx.db.dao.TbCityDao;
import com.example.emos.wx.service.HealthyService;
import com.example.emos.wx.utils.RiskUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Slf4j
@Scope("prototype")
public class HealthyServiceImpl implements HealthyService {

    @Autowired
    private TbCityDao cityDao;

    @Autowired
    private TbCheckinDao checkinDao;
    @Override
    public int getRiskAtLocation( CheckinForm form) {
        return RiskUtil.getRisk(cityDao.searchCode(form.getCity()),form);
    }

    @Override
    public int searchHighRiskCheckin(int userId) {
        return checkinDao.searchHighRiskCheckin(userId);
    }

    @Override
    public int searchMiddleRiskCheckin(int userId) {
        return checkinDao.searchMiddleRiskCheckin(userId);
    }

    @Override
    public int searchRiskCheckinCount(int userId, int risk) {
        HashMap map = new HashMap();
        map.put("userId",userId);
        map.put("risk",risk);
        return checkinDao.searchRiskCount(map);
    }
}
