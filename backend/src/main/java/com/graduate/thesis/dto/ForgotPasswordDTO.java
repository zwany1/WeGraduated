package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 忘记密码请求(密保问答)
 */
@Data
public class ForgotPasswordDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密保答案不能为空")
    private String answer;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为6-64位")
    private String newPassword;
}
