package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求: 账号可为用户名或邮箱
 */
@Data
public class LoginDTO {

    /** 用户名或邮箱 */
    @NotBlank(message = "请输入用户名或邮箱")
    @Size(max = 128, message = "账号长度过长")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为6-64位")
    private String password;

    /** 图形验证码 id */
    private String captchaId;

    /** 图形验证码内容 */
    private String captchaCode;
}
