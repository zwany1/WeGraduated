package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置密码请求(邮箱验证码)
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度过长")
    private String email;

    /** 邮箱验证码 */
    private String emailCode;

    @NotBlank(message = "请完成图形验证码")
    private String captchaId;

    @NotBlank(message = "请完成图形验证码")
    private String captchaCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为6-64位")
    private String newPassword;
}
