package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.ChangeEmailDTO;
import com.graduate.thesis.dto.ChangePasswordDTO;
import com.graduate.thesis.dto.LoginDTO;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.ResetPasswordDTO;
import com.graduate.thesis.dto.SendEmailCodeDTO;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.dto.UserInfoResponse;
import com.graduate.thesis.dto.UserProfileDTO;
import com.graduate.thesis.service.CaptchaService;
import com.graduate.thesis.service.EmailCodeService;
import com.graduate.thesis.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final CaptchaService captchaService;
    private final EmailCodeService emailCodeService;

    public UserController(UserService userService,
                          CaptchaService captchaService,
                          EmailCodeService emailCodeService) {
        this.userService = userService;
        this.captchaService = captchaService;
        this.emailCodeService = emailCodeService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody UserAuthDTO dto) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        emailCodeService.verify(dto.getEmail(), dto.getEmailCode());
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginDTO dto,
                                       HttpServletRequest request) {
        // 失败次数达到阈值才要求图形验证码, 降低正常用户登录摩擦
        if (userService.needCaptcha(dto.getAccount())) {
            captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        }
        return Result.ok(userService.login(dto, clientIp(request)));
    }

    /** 查询账号是否需要图形验证码(免登录, 供前端按需显示验证码) */
    @GetMapping("/need-captcha")
    public Result<Boolean> needCaptcha(@RequestParam("account") String account) {
        return Result.ok(userService.needCaptcha(account));
    }

    /** 发送邮箱验证码 */
    @PostMapping("/send-email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeDTO dto) {
        // 场景校验: 提前拦截"邮箱与账号不匹配", 避免验证码发出后重置/注册才失败
        if ("reset".equals(dto.getScene()) && !userService.existsByEmail(dto.getEmail())) {
            throw new com.graduate.thesis.common.BusinessException("该邮箱未绑定任何账号，请确认注册时使用的邮箱；未绑定邮箱的账号无法通过邮箱重置密码");
        }
        if ("register".equals(dto.getScene()) && userService.existsByEmail(dto.getEmail())) {
            throw new com.graduate.thesis.common.BusinessException("该邮箱已被注册，请直接登录或更换邮箱");
        }
        emailCodeService.sendCode(dto.getEmail());
        return Result.ok(null);
    }

    /** 忘记密码: 邮箱验证码重置并重新签发 token */
    @PostMapping("/reset-password")
    public Result<LoginResponse> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        return Result.ok(userService.resetPassword(dto));
    }

    /** 退出登录: 撤销当前 token */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        userService.logout(UserContext.get(), token);
        return Result.ok(null);
    }

    /** 注销账号: 永久删除账号及全部数据 */
    @DeleteMapping("/account")
    public Result<Void> deleteAccount(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        userService.deleteAccount(UserContext.get());
        if (token != null && !token.isEmpty()) {
            userService.logout(UserContext.get(), token);
        }
        return Result.ok(null);
    }

    /** 获取当前用户资料 */
    @GetMapping("/profile")
    public Result<UserProfileDTO> profile() {
        return Result.ok(userService.getProfile(UserContext.get()));
    }

    /** 获取当前用户角色与权限标识 */
    @GetMapping("/info")
    public Result<UserInfoResponse> info() {
        return Result.ok(userService.getUserInfo(UserContext.get()));
    }

    /** 更新当前用户资料(昵称/头像/简介/性别/学校/学院/城市/电话) */
    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@RequestBody UserProfileDTO dto) {
        return Result.ok(userService.updateProfile(UserContext.get(), dto));
    }

    /** 换绑邮箱: 新邮箱 + 新邮箱验证码 + 当前密码 */
    @PostMapping("/change-email")
    public Result<Void> changeEmail(@RequestBody ChangeEmailDTO dto) {
        userService.changeEmail(UserContext.get(), dto.getNewEmail(), dto.getCode(), dto.getCurrentPassword());
        return Result.ok(null);
    }

    /** 修改密码: 当前密码 + 当前邮箱验证码 + 新密码 */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.get(), dto.getCurrentPassword(), dto.getNewPassword(), dto.getEmailCode());
        return Result.ok(null);
    }

    /** 获取客户端 IP */
    private String clientIp(HttpServletRequest request) {
        return com.graduate.thesis.util.IpUtils.clientIp(request);
    }
}
