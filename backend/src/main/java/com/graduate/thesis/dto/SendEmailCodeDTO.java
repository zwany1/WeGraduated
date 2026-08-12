package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发送邮箱验证码请求
 */
@Data
public class SendEmailCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度过长")
    private String email;

    /** 图形验证码 id */
    private String captchaId;

    /** 图形验证码内容 */
    private String captchaCode;
}
