package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbMeetingApproval;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbMeetingApprovalDao {
    public void insertApproval(TbMeetingApproval approval);

    void deleteApprovalByUUID(String uuid);

    ArrayList<HashMap> searchNeedApprovalMeeting(int userId);

    ArrayList<HashMap> searchAlreadyApprovalMeeting(int userId);

    TbMeetingApproval searchApprovalByUUID(String uuid);

    int updateApprovals(HashMap hashMap);
}