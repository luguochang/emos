package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbMeetingApprovalDao;
import com.example.emos.wx.db.dao.TbMeetingDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.db.pojo.TbMeetingApproval;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MeetingService;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class MeetingServiceImpl implements MeetingService {
    @Autowired
    private TbMeetingDao meetingDao;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private MessageTask messageTask;

    @Autowired
    private TbMeetingApprovalDao approvalDao;

    private void startMeetingWorkflow(long meetingId,String uuid,long creatorId,String title){
        TbMeetingApproval approval = new TbMeetingApproval();
        approval.setUuid(uuid);

        Integer managerId = userDao.searchDeptManagerIdByUserId((int) creatorId);
        Integer gmId = userDao.searchGmId();
        Set members = new HashSet();

        JSONArray json_approvals = new JSONArray();

        if (creatorId == gmId){
            //如果创建者是总经理

            members.add((int)creatorId);
            json_approvals.put(creatorId);
            HashMap map = new HashMap();
            map.put("id",meetingId);
            map.put("status",3);
            meetingDao.updateMeetingStatus(map);
        }else {

            //给参与会议的成员的部门经理发审批
            //查询所有部门
            ArrayList<Integer> list = meetingDao.searchMeetingDepts(uuid);
            for (Integer deptId : list) {
                Integer deptMgrId = userDao.searchDeptManagerIdByDeptId(deptId);
                if (deptMgrId != null) {
                    members.add((int)deptMgrId);
                    sendMeetingNeedApprovalMsg(deptId,title);
                }
            }
            //如果创建者是部门经理
            if (creatorId == managerId){
                members.add((int)creatorId);
                approval.setLast_user((long) creatorId);
                json_approvals.put(creatorId);
                if (list.size() == 1) {
                    HashMap map = new HashMap();
                    map.put("id", meetingId);
                    map.put("status", 3);
                    meetingDao.updateMeetingStatus(map);
                }
            }
        }


        approval.setMembers(members.toString());
        approval.setApprovals(json_approvals.toString());
        approvalDao.insertApproval(approval);




    }

    private void sendMeetingNeedApprovalMsg(Integer deptManagerId, String title) {
        MessageEntity entity = new MessageEntity();
        entity.setSenderId(0);
        entity.setSenderName("会议审批");
        entity.setUuid(IdUtil.simpleUUID());
        entity.setMsg("您有一条会议("+title+")待审批");
        entity.setSendTime(new Date());
        messageTask.sendAsync(deptManagerId+ "", entity);
    }


    @Override
    public void insertMeeting(TbMeeting entity) {
        int row = meetingDao.insertMeeting(entity);
        long id = meetingDao.searchMeetingIdByUuid(entity.getUuid());

        if (row != 1){
            throw new EmosException("会议添加失败");
        }
        //todo 开启审批工作流
        startMeetingWorkflow(id, entity.getUuid(), entity.getCreatorId(),entity.getTitle());
    }

    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param) {

        ArrayList<HashMap> list = meetingDao.searchMyMeetingListByPage(param);
        //按日期进行分组  [{},{},..,..,{}]
        String date = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap = null;  //一个会议小列表  里面含date,list(json信息)
        JSONArray array = null;
        for (HashMap one : list){
            String temp = one.get("date").toString();

            //不同日期时新建一个map进行分组
            if (!temp.equals(date)){
                date = temp;
                resultMap = new HashMap();
                array = new JSONArray();

                resultMap.put("date",date);
                resultMap.put("list",array);
                resultList.add(resultMap);
            }
            array.put(one);
        }
        return resultList;
    }

    @Override
    public HashMap searchMeetingById(int meetingId) {
        HashMap map = meetingDao.searchMeetingById(meetingId);
        ArrayList<HashMap> list = meetingDao.searchMeetingMembers(meetingId);
        map.put("members", list);
        return map;
    }

    @Override
    public void updateMeetingInfo(HashMap param) {
        int id = (int) param.get("id");
        HashMap oldMeeting = meetingDao.searchMeetingById(id);
        String instanceId = oldMeeting.get("instanceId").toString();
        Integer creatorId = Integer.parseInt(oldMeeting.get("creatorId").toString());


        int row = meetingDao.updateMeetingInfo(param);
        if (row != 1) {
            throw new EmosException("会议更新失败");
        }


        // TODO: 2021/2/23 实现工作流
        approvalDao.deleteApprovalByUUID(instanceId);

        String newInstanceId = (String) param.get("instanceId");
        //创建新的工作流
        startMeetingWorkflow(id, newInstanceId, creatorId,oldMeeting.get("title").toString());
    }

    @Override
    public void deleteMeetingById(Integer id) {
        HashMap meeting = meetingDao.searchMeetingById(id); //查询会议信息
        String uuid = meeting.get("uuid").toString();

        //String instanceId = meeting.get("instanceId").toString();


        DateTime date = DateUtil.parse(meeting.get("date") + " " + meeting.get("start"));
        DateTime now = DateUtil.date();
        //会议开始前20分钟，不能删除会议
        if (now.isAfterOrEquals(date.offset(DateField.MINUTE, -20))) {
            throw new EmosException("距离会议开始不足20分钟，不能删除会议");
        }
        int row = meetingDao.deleteMeetingById(id);
        if (row != 1) {
            throw new EmosException("会议删除失败");
        }

        // // TODO: 2021/2/23 删除工作流

        approvalDao.deleteApprovalByUUID(uuid);
    }

    @Override
    public ArrayList searchNeedApprovalMeeting(int userId) {

        return approvalDao.searchNeedApprovalMeeting(userId);
    }

    @Override
    public ArrayList searchAlreadyApprovalMeeting(int userId) {
        return approvalDao.searchAlreadyApprovalMeeting(userId);
    }


    @Override
    public int approvalMeeting(HashMap hashMap) {

        int userId = (int) hashMap.get("userId");
        String uuid = hashMap.get("uuid").toString();
        int option = (int) hashMap.get("option");


        TbMeetingApproval approval = approvalDao.searchApprovalByUUID(uuid);
        JSONArray jsonMembers = JSONUtil.parseArray(approval.getMembers());
        JSONArray jsonApprovals;
        if (approval.getApprovals() == null || approval.getApprovals().toString().length() == 0) {
            jsonApprovals = new JSONArray();
        } else {
            jsonApprovals = JSONUtil.parseArray(approval.getApprovals());
        }

        boolean isSingleDept = jsonMembers.size() == 1;
        //非法审批
        if (isSingleDept) {
            if (!jsonMembers.contains(userId)) {
                return 0;
            }
        }
        //重复审批
        if (jsonApprovals.contains(userId)) {
            return -1;
        }

        Integer gmId = userDao.searchGmId();


        //单部门会议
        if (isSingleDept) {
            updateMeetingStatusByUUID(uuid, option);
            jsonApprovals.put(userId);
        } else {

            //多部门会议
            //总经理审批
            if (userId == gmId) {
                //就差总经理
                if (jsonApprovals.size() == jsonMembers.size()) {
                    updateMeetingStatusByUUID(uuid, option);
                } else {
                    return -2;
                }
            }else{
                jsonApprovals.put(userId);
            }
        }

        hashMap.put("approvals", jsonApprovals.toString());
        int result =  approvalDao.updateApprovals(hashMap);
        if(jsonApprovals.size() == jsonMembers.size() && option == 1 && result > 0){
            HashMap meetingInfo = meetingDao.searchMeetingByUUID(uuid);
            sendMeetingNeedApprovalMsg(gmId,meetingInfo.get("title").toString());
        }

        return result;

    }


    private void updateMeetingStatusByUUID(String uuid, int option) {
        HashMap param = new HashMap();
        param.put("uuid", uuid);
        param.put("status", option == 0 ? 2 : 3);
        meetingDao.updateMeetingStatusByUUID(param);
    }


    @Override
    public Integer searchMeetingStatus(String uuid) {
        return (Integer) meetingDao.searchMeetingStatus(uuid).get("status");
    }


}
