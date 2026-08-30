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

    /** 场景: register=注册(要求邮箱未被占用), reset=重置密码(要求邮箱已绑定账号); 缺省不校验 */
    private String scene;
}
