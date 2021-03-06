package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.TbMeeting;

import java.util.ArrayList;
import java.util.HashMap;

public interface MeetingService {
    public void insertMeeting(TbMeeting entity);

    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param);

    public HashMap searchMeetingById(int id);

    public void updateMeetingInfo(HashMap param);

    void deleteMeetingById(Integer id);

    ArrayList searchNeedApprovalMeeting(int userId);

    ArrayList searchAlreadyApprovalMeeting(int userId);

    Integer searchMeetingStatus(String uuid);

    int approvalMeeting(HashMap hashMap);
}
