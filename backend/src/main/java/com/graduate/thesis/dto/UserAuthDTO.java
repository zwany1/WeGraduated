package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录/注册请求
 */
@Data
public class UserAuthDTO {

    /** 用户名(选填) */
    @Size(min = 3, max = 32, message = "用户名长度为3-32位")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度过长")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为6-64位")
    private String password;
}
