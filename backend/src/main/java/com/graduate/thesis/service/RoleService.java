package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.dto.admin.AssignMenusDTO;
import com.graduate.thesis.dto.admin.RoleSaveDTO;
import com.graduate.thesis.dto.admin.RoleVO;
import com.graduate.thesis.entity.Role;
import com.graduate.thesis.entity.RoleMenu;
import com.graduate.thesis.entity.UserRole;
import com.graduate.thesis.mapper.RoleMapper;
import com.graduate.thesis.mapper.RoleMenuMapper;
import com.graduate.thesis.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 */
@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper,
                       RoleMenuMapper roleMenuMapper,
                       UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public PageResult<RoleVO> listRoles(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Role::getRoleName, kw).or().like(Role::getRoleKey, kw));
        }
        wrapper.orderByAsc(Role::getId);
        IPage<Role> page = roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<RoleVO> vos = toVOs(page.getRecords());
        return PageResult.of(page.getTotal(), vos);
    }

    /** 全部角色(供分配下拉框使用) */
    public List<RoleVO> listAllRoles() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .orderByAsc(Role::getId));
        return toVOs(roles);
    }

    private List<RoleVO> toVOs(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
        Map<Long, Long> userCount = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .in(UserRole::getRoleId, roleIds))
                .stream().collect(Collectors.groupingBy(UserRole::getRoleId, Collectors.counting()));

        List<RoleVO> result = new ArrayList<>();
        for (Role r : roles) {
            RoleVO vo = new RoleVO();
            vo.setId(r.getId());
            vo.setRoleName(r.getRoleName());
            vo.setRoleKey(r.getRoleKey());
            vo.setRemark(r.getRemark());
            vo.setStatus(r.getStatus());
            vo.setUserCount(userCount.getOrDefault(r.getId(), 0L).intValue());
            vo.setCreateTime(r.getCreateTime());
            vo.setUpdateTime(r.getUpdateTime());
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public void createRole(RoleSaveDTO dto) {
        validate(dto, true);
        Role role = new Role();
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setRemark(dto.getRemark());
        role.setStatus(dto.getStatus() == null ? Boolean.TRUE : dto.getStatus());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
    }

    @Transactional
    public void updateRole(RoleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "缺少角色ID");
        }
        Role role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        validate(dto, false);
        role.setRoleName(dto.getRoleName());
        role.setRemark(dto.getRemark());
        role.setStatus(dto.getStatus() == null ? Boolean.TRUE : dto.getStatus());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
    }

    /** 删除角色: 超管角色禁止删除; 同步清理用户角色与角色菜单关联 */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if (Role.KEY_ADMIN.equals(role.getRoleKey())) {
            throw new BusinessException(400, "内置超级管理员角色不可删除");
        }
        if (Role.KEY_USER.equals(role.getRoleKey())) {
            throw new BusinessException(400, "内置普通用户角色不可删除");
        }
        roleMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
    }

    /** 角色已分配的菜单ID(用于回显树勾选) */
    public List<Long> getRoleMenuIds(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if (Role.KEY_ADMIN.equals(role.getRoleKey())) {
            // 超管默认拥有全部菜单(返回空代表全选由前端处理)
            return Collections.emptyList();
        }
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }

    /** 给角色分配菜单 */
    @Transactional
    public void assignMenus(AssignMenusDTO dto) {
        if (dto.getRoleId() == null) {
            throw new BusinessException(400, "缺少角色ID");
        }
        Role role = roleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        List<Long> menuIds = dto.getMenuIds() == null ? Collections.emptyList() : dto.getMenuIds();
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, dto.getRoleId()));
        for (Long menuId : menuIds) {
            if (menuId != null) {
                roleMenuMapper.insert(new RoleMenu(dto.getRoleId(), menuId));
            }
        }
    }

    private void validate(RoleSaveDTO dto, boolean creating) {
        if (!StringUtils.hasText(dto.getRoleName())) {
            throw new BusinessException(400, "角色名称不能为空");
        }
        if (!StringUtils.hasText(dto.getRoleKey())) {
            throw new BusinessException(400, "角色权限字符串不能为空");
        }
        String key = dto.getRoleKey().trim();
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, key)
                .ne(creating, Role::getId, -1)
                .ne(dto.getId() != null, Role::getId, dto.getId()));
        if (count != null && count > 0) {
            throw new BusinessException(400, "角色权限字符串已存在");
        }
        dto.setRoleKey(key);
    }
}
