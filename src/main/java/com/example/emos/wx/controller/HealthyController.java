package com.example.emos.wx.controller;



import com.example.emos.wx.common.utils.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.controller.form.GetRiskCheckCountForm;
import com.example.emos.wx.service.HealthyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@RestController
@RequestMapping("/healthy")
@Api("健康接口")
public class HealthyController {

    @Autowired
    private HealthyService healthyService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/riskAtLocation")
    @ApiOperation("获取当前位置风险等级")
    public R getRiskAtLocation(@Valid @RequestBody CheckinForm form) {
        return R.ok().put("result", healthyService.getRiskAtLocation(form));

    }

    @PostMapping("/getRiskCheckCount")
    @ApiOperation("获取当前位置风险等级")
    public R getRiskCheckCount(@RequestHeader("token") String token,@Valid @RequestBody GetRiskCheckCountForm form) {

        int risk = form.getRisk();
        int userId = jwtUtil.getUserId(token);
        return R.ok().put("result", healthyService.searchRiskCheckinCount(userId,form.getRisk()));

    }



}
