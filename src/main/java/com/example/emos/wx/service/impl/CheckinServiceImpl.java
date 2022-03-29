package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemContants;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import com.sun.org.apache.bcel.internal.generic.ARETURN;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Slf4j
@Scope("prototype")
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SystemContants systemContants;

    @Autowired
    private TbHolidaysDao tbHolidaysDao;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Autowired
    private TbCityDao cityDao;

    @Value("${emos.email.hr}")
    private String hrEmail;

    @Autowired
    private EmailTask emailTask;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = tbHolidaysDao.searchTodayIsHoliday() != null ? true : false;
        boolean bool_2 = workdayDao.searchTodayIsWorkDay() != null ? true : false;
        String type = "工作日";
        if (DateUtil.date().isWeekend()){
            type = "节假日";
        }
        if (bool_1){
            type = "节假日";
        }else if (bool_2){
            type = "工作日";
        }

        if (type.equals("节假日")){
            return "节假日不需要签到";
        }else{
            DateTime now = DateUtil.date();
            String test = DateUtil.today();
            String start = DateUtil.today() + " " + systemContants.attendanceStartTime;
            String  end  = DateUtil.today() + " " + systemContants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)){
                return "没有到上班考勤开始时间";
            }else if (now.isAfter(attendanceEnd)){
                return "超过了上班考勤结束时间";
            }else {
                HashMap map = new HashMap();
                map.put("userId",userId);
                map.put("date",date);
                map.put("start",start);
                map.put("end",end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已经考勤，不用重复考勤" : "可以考勤";
            }

        }
    }

    @Override
    public void checkin(CheckinForm form, int userId, String path) {
        DateTime d1 = DateUtil.date(); //当前时间
        DateTime d2 = DateUtil.parse(DateUtil.today() + " " + systemContants.attendanceTime);//上班时间
        DateTime d3 = DateUtil.parse(DateUtil.today() + " " + systemContants.attendanceEndTime);

        int status = 1;
        if (d1.compareTo(d2) <= 0 ){
            status =1;//正常签到
        }else if (d1.compareTo(d2) > 0 && d1.compareTo(d3)<0){
            status =2;//迟到
        }

        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel==null){
            throw  new EmosException("不存在人脸模型");
        }else {
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path),"userId",userId);
            HttpResponse response = request.execute();
            if (response.getStatus()!=200){
                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
            String body = response.body();
            if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)){
                throw new EmosException(body);
            }else if ("False".equals(body)){
                throw new EmosException("签到无效，非本人签到");
            }else if ("True".equals(body)){
                //TODO 获取签到地区风险等级
                int risk = 1;
                if (form.getCity() != null && form.getCity().length()>0
                && form.getDistrict()!=null && form.getDistrict().length()>0){
                    String code = cityDao.searchCode(form.getCity());
                    //查询地区风险
                    try{
                        String url =  "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" +
                                form.getDistrict();
                        Document document = Jsoup.connect(url).get();
                        Elements elements = document.getElementsByClass("list-detail");
                        for (Element e : elements){
                            String result = e.text().split(" ")[1];
                            if ("高风险".equals(result)){
                                risk = 3;
                                //TODO 发送告警邮件
                                HashMap map = userDao.searchNameAndDept(userId);
                                String name = (String) map.get("name");
                                String dept_name = (String) map.get("dept_name");
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("员工"+name+"身处高风险疫情地区警告");
                                message.setText(dept_name+"员工"+name+","+DateUtil.format(new Date(),"yyyy年MM月dd日")
                                +"处于"+form.getAddress()+",属于新冠疫情高风险地区，请及时与该员工联系，何时情况!");
                                emailTask.sendAsync(message);
                            }else if ("中风险".equals(result)){
                                risk = risk<2 ? 2 :risk;
                            }
                        }

                    }catch (IOException e){
                        log.error("执行异常",e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
//              //保存签到记录
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(form.getAddress());
                entity.setCountry(form.getCountry());
                entity.setProvince(form.getProvince());
                entity.setCity(form.getCity());
                entity.setDistrict(form.getDistrict());
                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
        }

    }

    @Override
    public void createFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        File file = FileUtil.file(path);
        request.form("photo", file);
        request.form("userId",userId);
        HttpResponse response = request.execute();
        String body = response.body();

        if ("无法识别出人脸".equals(body)||"照片中存在多张人脸".equals(body)){
            throw new EmosException(body);
        }else {
            TbFaceModel faceModel = new TbFaceModel();
            faceModel.setUserId(userId);
            faceModel.setFaceModel(body);
            faceModelDao.insert(faceModel);
        }

    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        return checkinDao.searchTodayCheckin(userId);
    }

    @Override
    public long searchCheckinDays(int userId) {
        return checkinDao.searchCheckinDays(userId);
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap map) {
        //map中的参数为 userId、startDate、endDate;
        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(map);
        ArrayList<String> holidaysList = tbHolidaysDao.searchHolidaysInRange(map);
        ArrayList<String> workdayList = workdayDao.searchWorkDayInRange(map);

        DateTime startDate = DateUtil.parseDate(map.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(map.get("endDate").toString());

        //生成本周的七天对象
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);

        ArrayList list = new ArrayList();
        range.forEach(one->{
            String date = one.toString("yyyy-MM-dd");
            String type = "工作日";
            if (one.isWeekend()){
                type = "节假日";
            }
            if (holidaysList!= null &&  holidaysList.contains(date)){
                type = "节假日";
            }else if (workdayList!=null && workdayList.contains(date)){
                type = "工作日";
            }
            String status = "";

            if (type.equals("工作日") &&  DateUtil.compare(one, DateUtil.date()) <= 0){
                status = "缺勤";
                boolean flag = false;
                for (HashMap<String,String> checkinRecord : checkinList){
                    if (checkinRecord.containsValue(date)){
                        status = checkinRecord.get("status");
                        flag = true;
                        break;
                    }

                    //判断是否到今天签到的截止时间，如果没有就把状态设为空
                    DateTime endTime = DateUtil.parse(DateUtil.today()+" "+systemContants.attendanceEndTime);
                    String today = DateUtil.today();
                    if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag==false){
                        status = "";
                    }
                }

            }
            HashMap res = new HashMap();
            res.put("date",date);
            res.put("status",status);
            res.put("type",type);
            res.put("day",one.dayOfWeekEnum().toChinese("周"));
            list.add(res);


        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }
}
