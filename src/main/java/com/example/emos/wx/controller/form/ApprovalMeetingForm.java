package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;


@Data
@ApiModel
public class ApprovalMeetingForm {

    @NotBlank
    private String uuid;

    /**
     * 0 不同意
     * 1 同意
     */
    @Range(min = 0, max = 1)
    private int state;

}
