package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.entity.Team;
import com.graduate.thesis.entity.TeamMember;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.mapper.TeamMapper;
import com.graduate.thesis.mapper.TeamMemberMapper;
import com.graduate.thesis.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 团队协作服务: 创建团队/邀请成员/成员管理, 团队内共享模板/论文/任务。
 */
@Service
public class TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final FormatTemplateMapper templateMapper;

    public TeamService(TeamMapper teamMapper, TeamMemberMapper memberMapper,
                       UserMapper userMapper, FormatTemplateMapper templateMapper) {
        this.teamMapper = teamMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.templateMapper = templateMapper;
    }

    // ==================== 团队管理 ====================

    @Transactional
    public Team create(Long userId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("请输入团队名称");
        }
        Team team = new Team();
        team.setName(name.trim());
        team.setDescription(description == null || description.isEmpty() ? null : description.trim());
        team.setOwnerId(userId);
        team.setCreateTime(LocalDateTime.now());
        teamMapper.insert(team);
        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserId(userId);
        owner.setRole(Team.ROLE_OWNER);
        owner.setJoinTime(LocalDateTime.now());
        memberMapper.insert(owner);
        return team;
    }

    /** 我的团队(创建 + 加入), 附带我的角色与成员数 */
    public List<Map<String, Object>> listMyTeams(Long userId) {
        List<TeamMember> mine = memberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, userId));
        if (mine.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> teamIds = mine.stream().map(TeamMember::getTeamId).collect(Collectors.toList());
        Map<Long, Team> teams = teamMapper.selectList(new LambdaQueryWrapper<Team>().in(Team::getId, teamIds))
                .stream().collect(Collectors.toMap(Team::getId, t -> t, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>();
        for (TeamMember m : mine) {
            Team t = teams.get(m.getTeamId());
            if (t == null) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("description", t.getDescription());
            map.put("ownerId", t.getOwnerId());
            map.put("role", m.getRole());
            map.put("memberCount", countMembers(t.getId()));
            map.put("createTime", t.getCreateTime());
            out.add(map);
        }
        return out;
    }

    /** 团队详情(成员校验): 团队信息 + 成员列表 */
    public Map<String, Object> detail(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(404, "团队不存在");
        }
        requireMember(teamId, userId);
        List<TeamMember> members = memberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId).orderByAsc(TeamMember::getJoinTime));
        List<Long> userIds = members.stream().map(TeamMember::getUserId).collect(Collectors.toList());
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> memberList = new ArrayList<>();
        for (TeamMember m : members) {
            User u = users.get(m.getUserId());
            Map<String, Object> mm = new HashMap<>();
            mm.put("userId", m.getUserId());
            mm.put("username", u == null ? "-" : u.getUsername());
            mm.put("nickname", u == null ? "-" : (u.getNickname() == null ? u.getUsername() : u.getNickname()));
            mm.put("email", u == null ? null : u.getEmail());
            mm.put("role", m.getRole());
            mm.put("joinTime", m.getJoinTime());
            memberList.add(mm);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", team.getId());
        result.put("name", team.getName());
        result.put("description", team.getDescription());
        result.put("ownerId", team.getOwnerId());
        result.put("createTime", team.getCreateTime());
        result.put("members", memberList);
        return result;
    }

    /** 邀请成员: 按用户名或邮箱查找用户加入团队(需 owner) */
    public void invite(Long teamId, Long operatorId, String keyword) {
        requireOwner(teamId, operatorId);
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("请输入用户名或邮箱");
        }
        String kw = keyword.trim().toLowerCase();
        User target = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, kw)
                        .or().eq(User::getEmail, keyword.trim())));
        if (target == null) {
            throw new BusinessException(404, "未找到该用户，请确认用户名或邮箱正确");
        }
        if (target.getId().equals(operatorId)) {
            throw new BusinessException("不能邀请自己");
        }
        Long exists = memberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, target.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该用户已在团队中");
        }
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(target.getId());
        member.setRole(Team.ROLE_MEMBER);
        member.setJoinTime(LocalDateTime.now());
        memberMapper.insert(member);
    }

    /** 移除成员(需 owner; owner 本人不可被移除) */
    public void removeMember(Long teamId, Long operatorId, Long userId) {
        requireOwner(teamId, operatorId);
        if (userId.equals(operatorId)) {
            throw new BusinessException("请使用解散团队");
        }
        TeamMember member = memberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(404, "该成员不在团队中");
        }
        memberMapper.deleteById(member.getId());
        // 移除后, 该成员在团队资源上无权限(通过 isMember 校验自动失效)
    }

    /** 退出团队(owner 不可退出, 需解散) */
    public void leave(Long teamId, Long userId) {
        requireMember(teamId, userId);
        Team team = teamMapper.selectById(teamId);
        if (team != null && team.getOwnerId().equals(userId)) {
            throw new BusinessException("队长不能退出团队，可解散团队");
        }
        memberMapper.delete(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, userId));
    }

    /** 解散团队(需 owner): 团队模板归还创建者个人 */
    @Transactional
    public void deleteTeam(Long teamId, Long operatorId) {
        requireOwner(teamId, operatorId);
        List<TeamMember> members = memberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId));
        // 团队内模板归还其创建者
        for (TeamMember m : members) {
            templateMapper.update(null, new LambdaUpdateWrapper<FormatTemplate>()
                    .eq(FormatTemplate::getTeamId, teamId)
                    .eq(FormatTemplate::getUserId, m.getUserId())
                    .set(FormatTemplate::getTeamId, null));
        }
        memberMapper.delete(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
        teamMapper.deleteById(teamId);
    }

    // ==================== 权限辅助 ====================

    /** 我所在的团队 id 列表 */
    public List<Long> myTeamIds(Long userId) {
        return memberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId))
                .stream().map(TeamMember::getTeamId).collect(Collectors.toList());
    }

    /** 是否团队成员 */
    public boolean isMember(Long teamId, Long userId) {
        if (teamId == null) {
            return false;
        }
        Long c = memberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId).eq(TeamMember::getUserId, userId));
        return c != null && c > 0;
    }

    private void requireMember(Long teamId, Long userId) {
        if (!isMember(teamId, userId)) {
            throw new BusinessException(403, "您不是该团队成员");
        }
    }

    private void requireOwner(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(404, "团队不存在");
        }
        if (!team.getOwnerId().equals(userId)) {
            throw new BusinessException(403, "仅团队队长可执行此操作");
        }
    }

    private long countMembers(Long teamId) {
        Long c = memberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId));
        return c == null ? 0 : c;
    }
}
