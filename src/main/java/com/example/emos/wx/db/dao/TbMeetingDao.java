package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbMeeting;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface TbMeetingDao {
    public int insertMeeting(TbMeeting entity);

    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param);

    public boolean searchMeetingMembersInSameDept(String uuid);

    public HashMap searchMeetingById(int meetingId);

    public ArrayList<HashMap> searchMeetingMembers(int meetingId);

    public void updateMeetingStatus(HashMap map);

    public int updateMeetingInfo(HashMap map);

    public int deleteMeetingById(int id);

    public long searchMeetingIdByUuid(String uuid);

    ArrayList<Integer> searchMeetingDepts(String uuid);


    void updateMeetingStatusByUUID(HashMap param);

    HashMap searchMeetingStatus(String uuid);

    HashMap searchMeetingByUUID(String uuid);
}