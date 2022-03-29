package com.example.emos.wx.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.example.emos.wx.common.utils.R;
import com.example.emos.wx.config.SystemContants;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.controller.form.SearchMonthCheckinForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.service.impl.CheckinServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@Api("签到模块web接口")
@Slf4j
@RequestMapping("/checkin")
public class CheckinController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CheckinService checkinService;

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private SystemContants contants;

    @Autowired
    private UserService userService;


    @GetMapping("/validCanCheckin")
    @ApiOperation("查看用户今天是否可以签到")
    public R validCanCheckin(@RequestHeader("token") String token){

        int userId = jwtUtil.getUserId(token);
        String result = checkinService.validCanCheckIn(userId, DateUtil.today());
        return R.ok(result);
    }

    @PostMapping("/checkin")
    @ApiOperation("用户签到模块")
    public R checkin(@RequestHeader("token") String token, @Valid CheckinForm form, @RequestParam("photo")MultipartFile file){
        if (null == file){
            return R.error("没有上传文件");
        };
        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        String path = imageFolder + "/" + fileName;
        if (!fileName.endsWith(".jpg")){
            FileUtil.del(path);
            return R.error("必须提交JPG格式图片");
        }else{
            try {
                file.transferTo(Paths.get(path));

                checkinService.checkin(form,userId,path);
                return R.ok("签到成功");
            }catch (IOException e){
                log.error(e.getMessage());
                throw new EmosException("图片保存错误");
            }finally {
                FileUtil.del(path);
            }
        }
    }


    @PostMapping("/createFaceModel")
    @ApiOperation("创建人脸模型")
    public R createFaceModel(@RequestParam("photo") MultipartFile file,@RequestHeader("token") String token){
        int userId = jwtUtil.getUserId(token);
        if (file==null){
            return R.error("没有上传文件");
        }
        String fileName = file.getOriginalFilename().toLowerCase();
        String path = imageFolder+"/"+fileName;
        if (!fileName.endsWith(".jpg")){
            return R.error("必须提交JPG格式图片");
        }
        try {

            file.transferTo(Paths.get(path));
            checkinService.createFaceModel(userId,path);
            return R.ok("人脸建模成功");
        }catch (IOException e){
            log.error(e.getMessage());
            throw new EmosException("保存图片错误");
        }finally {
            FileUtil.del(path);
        }


    }


    @ApiOperation("查询用户当日的签到数据情况")
    @GetMapping("/searchTodayCheckin")
    public R searchTodayCheckin(@RequestHeader("token")String token){
        int userId = jwtUtil.getUserId(token);
        //查询出个人信息和当日的签到信息
        HashMap map = checkinService.searchTodayCheckin(userId);

        map.put("attendanceTime",contants.attendanceTime);
        map.put("closingTime",contants.closingTime);

        long days = checkinService.searchCheckinDays(userId);
        map.put("checkinDays",days);

        //判断日期是否在用户入职前
        DateTime hiredate = DateUtil.parse(userService.searchHireDate(userId));
        DateTime startDate = DateUtil.beginOfWeek(DateUtil.date());
        if (startDate.isBefore(hiredate)){
            startDate = hiredate;
        }
        DateTime endDate = DateUtil.endOfWeek(DateUtil.date());

        HashMap param = new HashMap();
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        param.put("userId",userId);

        ArrayList<HashMap> list = checkinService.searchWeekCheckin(param);
        map.put("weekCheckin",list);
        return R.ok().put("result",map);
    }


    @PostMapping("/searchMonthCheckin")
    @ApiOperation("/查询用户某月签到数据")
    public R searchMonthCheckin(@Valid @RequestBody SearchMonthCheckinForm form,@RequestHeader("token")String token){
        int userId = jwtUtil.getUserId(token);
        DateTime hiredate = DateUtil.parse(userService.searchHireDate(userId));
        String month = form.getMonth()<0 ? "0"+form.getMonth() : ""+form.getMonth();

        DateTime startDate = DateUtil.parse(form.getYear() + "-" + month + "-01");

        //如果查询的月份早于员工入职的月份就抛出异常
        if (startDate.isBefore(DateUtil.beginOfMonth(hiredate))){
            throw new EmosException("只能查询考勤之后的日趋数据");
        }
        //如果查询的月份是入职月份，本月考勤开始时间设置为入职日期
        if (startDate.isBefore(hiredate)){
            startDate = hiredate;
        }
        DateTime endDate = DateUtil.endOfMonth(startDate);

        HashMap param = new HashMap();
        param.put("userId",userId);
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        ArrayList<HashMap> list = checkinService.searchMonthCheckin(param);

        //统计月考勤数据
        int sum_1=0,sum_2=0,sum_3=0 ;
        for (HashMap<String,String> map : list){
            String type = map.get("type");
            String status = map.get("status");
            if ("工作日".equals(type)){
                if ("正常".equals(status)){
                    sum_1++;
                }else if ("迟到".equals(status)){
                    sum_2++;
                }else if ("缺勤".equals(status)){
                    sum_3++;
                }
            }
        }
        return R.ok().put("list",list).put("sum_1",sum_1).put("sum_2",
                sum_2).put("sum_3", sum_3);

    }
}
