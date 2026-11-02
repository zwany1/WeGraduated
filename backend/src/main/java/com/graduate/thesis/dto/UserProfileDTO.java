package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户资料
 *
 * 字段约束与前端表单保持一致: 邮箱/手机号入库存了脏数据会影响找回密码与通知,
 * 所以必须在这里拦截, 不能只靠前端校验。
 */
@Data
public class UserProfileDTO {
    private Long userId;
    @Size(max = 32, message = "用户名过长")
    private String username;
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱过长")
    private String email;
    @Size(max = 32, message = "昵称最多 32 个字符")
    private String nickname;
    @Size(max = 512, message = "头像地址过长")
    private String avatar;
    @Size(max = 500, message = "个人简介最多 500 个字符")
    private String bio;
    @Min(value = 0, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    private Integer gender;
    @Size(max = 64, message = "学校名称过长")
    private String school;
    @Size(max = 64, message = "专业名称过长")
    private String major;
    @Size(max = 32, message = "城市名称过长")
    private String city;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
