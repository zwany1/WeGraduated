package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.entity.Team;
import com.graduate.thesis.service.TeamService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 团队协作接口
 */
@RestController
@RequestMapping("/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /** 创建团队 */
    @PostMapping("/create")
    public Result<Team> create(@RequestBody Map<String, Object> body) {
        String name = body.get("name") == null ? "" : String.valueOf(body.get("name"));
        String desc = body.get("description") == null ? null : String.valueOf(body.get("description"));
        return Result.ok(teamService.create(UserContext.get(), name, desc));
    }

    /** 我的团队 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(teamService.listMyTeams(UserContext.get()));
    }

    /** 团队详情(含成员) */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(teamService.detail(id, UserContext.get()));
    }

    /** 邀请成员(按用户名/邮箱) */
    @PostMapping("/{id}/invite")
    public Result<Void> invite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String keyword = body.get("keyword") == null ? "" : String.valueOf(body.get("keyword"));
        teamService.invite(id, UserContext.get(), keyword);
        return Result.ok();
    }

    /** 移除成员 */
    @DeleteMapping("/{id}/member/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        teamService.removeMember(id, UserContext.get(), userId);
        return Result.ok();
    }

    /** 退出团队 */
    @PostMapping("/{id}/leave")
    public Result<Void> leave(@PathVariable Long id) {
        teamService.leave(id, UserContext.get());
        return Result.ok();
    }

    /** 被邀请人同意加入 */
    @PostMapping("/invite/{inviteId}/accept")
    public Result<Void> acceptInvite(@PathVariable Long inviteId) {
        teamService.acceptInvite(inviteId, UserContext.get());
        return Result.ok();
    }

    /** 被邀请人拒绝 */
    @PostMapping("/invite/{inviteId}/reject")
    public Result<Void> rejectInvite(@PathVariable Long inviteId) {
        teamService.rejectInvite(inviteId, UserContext.get());
        return Result.ok();
    }

    /** 设置成员角色(admin/editor/viewer/member) */
    @PutMapping("/{id}/member/{userId}/role")
    public Result<Void> setRole(@PathVariable Long id, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        String role = body.get("role") == null ? null : String.valueOf(body.get("role"));
        teamService.setRole(id, UserContext.get(), userId, role);
        return Result.ok();
    }

    /** 转让队长所有权(原队长降为管理员) */
    @PostMapping("/{id}/transfer")
    public Result<Void> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long newOwnerId = body.get("newOwnerId") == null ? null : Long.valueOf(String.valueOf(body.get("newOwnerId")));
        teamService.transferOwnership(id, UserContext.get(), newOwnerId);
        return Result.ok();
    }

    /** 解散团队(队长) */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        teamService.deleteTeam(id, UserContext.get());
        return Result.ok();
    }
}
