package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.entity.Config;
import com.graduate.thesis.entity.DictData;
import com.graduate.thesis.entity.DictType;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.entity.Notice;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.entity.SiteCase;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.ConfigMapper;
import com.graduate.thesis.mapper.DictDataMapper;
import com.graduate.thesis.mapper.DictTypeMapper;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.mapper.NoticeMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import com.graduate.thesis.mapper.SiteCaseMapper;
import com.graduate.thesis.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置服务: 字典 / 系统参数 / 通知公告
 */
@Service
public class SystemService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;
    private final ConfigMapper configMapper;
    private final NoticeMapper noticeMapper;
    private final SiteCaseMapper siteCaseMapper;
    private final FormatTaskMapper taskMapper;
    private final UserMapper userMapper;
    private final FormatTemplateMapper templateMapper;
    private final PaperFileMapper paperFileMapper;
    private final StorageService storageService;

    public SystemService(DictTypeMapper dictTypeMapper,
                         DictDataMapper dictDataMapper,
                         ConfigMapper configMapper,
                         NoticeMapper noticeMapper,
                         SiteCaseMapper siteCaseMapper,
                         FormatTaskMapper taskMapper,
                         UserMapper userMapper,
                         FormatTemplateMapper templateMapper,
                         PaperFileMapper paperFileMapper,
                         StorageService storageService) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
        this.configMapper = configMapper;
        this.noticeMapper = noticeMapper;
        this.siteCaseMapper = siteCaseMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.templateMapper = templateMapper;
        this.paperFileMapper = paperFileMapper;
        this.storageService = storageService;
    }

    // ==================== 字典类型 ====================

    public PageResult<DictType> listDictTypes(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<DictType> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            w.and(x -> x.like(DictType::getDictName, kw).or().like(DictType::getDictType, kw));
        }
        w.orderByAsc(DictType::getId);
        IPage<DictType> page = dictTypeMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public List<DictType> listAllDictTypes() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<DictType>().orderByAsc(DictType::getId));
    }

    @Transactional
    public void saveDictType(DictType dto) {
        if (!StringUtils.hasText(dto.getDictName()) || !StringUtils.hasText(dto.getDictType())) {
            throw new BusinessException(400, "字典名称/类型不能为空");
        }
        if (dto.getStatus() == null) {
            dto.setStatus(true);
        }
        if (dto.getId() == null) {
            Long exists = dictTypeMapper.selectCount(new LambdaQueryWrapper<DictType>()
                    .eq(DictType::getDictType, dto.getDictType()));
            if (exists != null && exists > 0) {
                throw new BusinessException(400, "字典类型已存在");
            }
            dto.setCreateTime(LocalDateTime.now());
            dto.setUpdateTime(LocalDateTime.now());
            dictTypeMapper.insert(dto);
        } else {
            dto.setUpdateTime(LocalDateTime.now());
            dictTypeMapper.updateById(dto);
        }
    }

    @Transactional
    public void deleteDictType(Long id) {
        DictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        dictDataMapper.delete(new LambdaQueryWrapper<DictData>().eq(DictData::getDictType, type.getDictType()));
        dictTypeMapper.deleteById(id);
    }

    // ==================== 字典数据 ====================

    public List<DictData> listDictData(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, dictType)
                .eq(DictData::getStatus, true)
                .orderByAsc(DictData::getDictSort));
    }

    public PageResult<DictData> pageDictData(int pageNum, int pageSize, String dictType) {
        LambdaQueryWrapper<DictData> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictType)) {
            w.eq(DictData::getDictType, dictType);
        }
        w.orderByAsc(DictData::getDictSort);
        IPage<DictData> page = dictDataMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional
    public void saveDictData(DictData dto) {
        if (!StringUtils.hasText(dto.getDictType()) || !StringUtils.hasText(dto.getDictLabel())
                || !StringUtils.hasText(dto.getDictValue())) {
            throw new BusinessException(400, "字典类型/标签/值不能为空");
        }
        if (dto.getStatus() == null) {
            dto.setStatus(true);
        }
        if (dto.getDictSort() == null) {
            dto.setDictSort(0);
        }
        if (dto.getId() == null) {
            dto.setCreateTime(LocalDateTime.now());
            dto.setUpdateTime(LocalDateTime.now());
            dictDataMapper.insert(dto);
        } else {
            dto.setUpdateTime(LocalDateTime.now());
            dictDataMapper.updateById(dto);
        }
    }

    @Transactional
    public void deleteDictData(Long id) {
        dictDataMapper.deleteById(id);
    }

    // ==================== 系统参数 ====================

    public PageResult<Config> listConfigs(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Config> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            w.and(x -> x.like(Config::getConfigName, kw).or().like(Config::getConfigKey, kw));
        }
        w.orderByAsc(Config::getId);
        IPage<Config> page = configMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public String getConfigValue(String key) {
        Config c = configMapper.selectOne(new LambdaQueryWrapper<Config>().eq(Config::getConfigKey, key).last("LIMIT 1"));
        return c == null ? null : c.getConfigValue();
    }

    @Transactional
    public void saveConfig(Config dto) {
        if (!StringUtils.hasText(dto.getConfigName()) || !StringUtils.hasText(dto.getConfigKey())) {
            throw new BusinessException(400, "参数名称/键不能为空");
        }
        if (dto.getConfigType() == null) {
            dto.setConfigType(true);
        }
        if (dto.getId() == null) {
            dto.setCreateTime(LocalDateTime.now());
            dto.setUpdateTime(LocalDateTime.now());
            configMapper.insert(dto);
        } else {
            dto.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(dto);
        }
    }

    @Transactional
    public void deleteConfig(Long id) {
        Config c = configMapper.selectById(id);
        if (c != null && Boolean.TRUE.equals(c.getConfigType())) {
            throw new BusinessException(400, "内置参数不可删除");
        }
        configMapper.deleteById(id);
    }

    // ==================== 通知公告 ====================

    public PageResult<Notice> listNotices(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(Notice::getTitle, keyword.trim());
        }
        w.orderByDesc(Notice::getId);
        IPage<Notice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 前台展示的启用公告(前 N 条) */
    public List<Notice> listPublicNotices(int limit) {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, true)
                .orderByDesc(Notice::getId)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 20)));
    }

    @Transactional
    public void saveNotice(Notice dto, Long userId) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (dto.getNoticeType() == null) {
            dto.setNoticeType("1");
        }
        if (dto.getStatus() == null) {
            dto.setStatus(true);
        }
        if (dto.getId() == null) {
            dto.setCreateUser(userId);
            dto.setCreateTime(LocalDateTime.now());
            dto.setUpdateTime(LocalDateTime.now());
            noticeMapper.insert(dto);
        } else {
            dto.setUpdateTime(LocalDateTime.now());
            noticeMapper.updateById(dto);
        }
    }

    @Transactional
    public void deleteNotice(Long id) {
        noticeMapper.deleteById(id);
    }

    // ==================== 项目案例 ====================

    public PageResult<SiteCase> listCases(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<SiteCase> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(SiteCase::getTitle, keyword.trim());
        }
        w.orderByAsc(SiteCase::getSortOrder).orderByDesc(SiteCase::getId);
        IPage<SiteCase> page = siteCaseMapper.selectPage(new Page<>(pageNum, pageSize), w);
        enrichCases(page.getRecords());
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 前台展示的启用案例(按排序, 不含详情正文) */
    public List<SiteCase> listPublicCases(int limit) {
        List<SiteCase> list = siteCaseMapper.selectList(new LambdaQueryWrapper<SiteCase>()
                .select(SiteCase::getId, SiteCase::getTag, SiteCase::getTitle, SiteCase::getDescription,
                        SiteCase::getColor, SiteCase::getAuthor, SiteCase::getSchool, SiteCase::getRating,
                        SiteCase::getSortOrder, SiteCase::getVisible, SiteCase::getMetrics,
                        SiteCase::getTemplateId, SiteCase::getTaskId, SiteCase::getCreateTime)
                .eq(SiteCase::getVisible, true)
                .orderByAsc(SiteCase::getSortOrder)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 50)));
        enrichCases(list);
        return list;
    }

    /** 前台案例详情(含正文与截图, 派生真实信息) */
    public SiteCase getPublicCase(Long id) {
        SiteCase c = siteCaseMapper.selectById(id);
        if (c == null || !Boolean.TRUE.equals(c.getVisible())) {
            throw new BusinessException(404, "案例不存在");
        }
        enrichCases(java.util.Collections.singletonList(c));
        return c;
    }

    @Transactional
    public void saveCase(SiteCase dto, Long userId) {
        if (dto.getTaskId() != null) {
            FormatTask task = taskMapper.selectById(dto.getTaskId());
            if (task == null || !FormatTask.STATUS_SUCCESS.equals(task.getStatus())) {
                throw new BusinessException(400, "关联任务不存在或未完成");
            }
        }
        if (dto.getId() == null) {
            if (!StringUtils.hasText(dto.getTitle())) {
                throw new BusinessException(400, "标题不能为空");
            }
            if (dto.getColor() == null) {
                dto.setColor("blue");
            }
            if (dto.getRating() == null) {
                dto.setRating(java.math.BigDecimal.ZERO);
            }
            if (dto.getSortOrder() == null) {
                dto.setSortOrder(0);
            }
            if (dto.getVisible() == null) {
                dto.setVisible(true);
            }
            dto.setCreateUser(userId);
            dto.setCreateTime(LocalDateTime.now());
            dto.setUpdateTime(LocalDateTime.now());
            siteCaseMapper.insert(dto);
        } else {
            dto.setUpdateTime(LocalDateTime.now());
            siteCaseMapper.updateById(dto);
        }
    }

    @Transactional
    public void deleteCase(Long id) {
        siteCaseMapper.deleteById(id);
    }

    /** 案例关联的真实文档文件(仅真实案例) */
    public java.io.File getCaseFile(Long id) {
        SiteCase c = siteCaseMapper.selectById(id);
        if (c == null || c.getTaskId() == null) {
            throw new BusinessException(404, "案例无关联文档");
        }
        FormatTask task = taskMapper.selectById(c.getTaskId());
        if (task == null || task.getResultPath() == null) {
            throw new BusinessException(404, "案例文档已归档");
        }
        return storageService.load(task.getResultPath());
    }

    /** 候选上架任务(SUCCESS, 含派生信息) */
    public List<java.util.Map<String, Object>> listCandidateTasks() {
        List<FormatTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getStatus, FormatTask.STATUS_SUCCESS)
                .isNotNull(FormatTask::getResultPath)
                .orderByDesc(FormatTask::getFinishTime)
                .last("LIMIT 50"));
        if (tasks.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.Set<Long> userIds = tasks.stream().map(FormatTask::getUserId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> tplIds = tasks.stream().map(FormatTask::getTemplateId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> fileIds = tasks.stream().map(FormatTask::getFileId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, User> userMap = batchUsers(userIds);
        java.util.Map<Long, FormatTemplate> tplMap = batchTemplates(tplIds);
        java.util.Map<Long, PaperFile> fileMap = batchFiles(fileIds);
        List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (FormatTask t : tasks) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", t.getId());
            User u = userMap.get(t.getUserId());
            m.put("username", u == null ? null : u.getUsername());
            FormatTemplate tpl = tplMap.get(t.getTemplateId());
            m.put("templateName", tpl == null ? null : tpl.getName());
            PaperFile f = fileMap.get(t.getFileId());
            m.put("originalName", f == null ? null : f.getOriginalName());
            m.put("finishTime", t.getFinishTime());
            m.put("minutes", minutesOf(t));
            out.add(m);
        }
        return out;
    }

    /** 批量填充案例派生字段(真实任务: 用户/模板/文档/耗时; 手写: 试用入口) */
    private void enrichCases(List<SiteCase> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> taskIds = list.stream().map(SiteCase::getTaskId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
        if (taskIds.isEmpty()) {
            list.forEach(this::enrichManual);
            return;
        }
        java.util.Map<Long, FormatTask> taskMap = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                        .in(FormatTask::getId, taskIds))
                .stream().collect(java.util.stream.Collectors.toMap(FormatTask::getId, t -> t, (a, b) -> a));
        java.util.Set<Long> userIds = taskMap.values().stream().map(FormatTask::getUserId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> tplIds = taskMap.values().stream().map(FormatTask::getTemplateId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> fileIds = taskMap.values().stream().map(FormatTask::getFileId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, User> userMap = batchUsers(userIds);
        java.util.Map<Long, FormatTemplate> tplMap = batchTemplates(tplIds);
        java.util.Map<Long, PaperFile> fileMap = batchFiles(fileIds);
        for (SiteCase c : list) {
            if (c.getTaskId() == null) {
                enrichManual(c);
                continue;
            }
            FormatTask t = taskMap.get(c.getTaskId());
            if (t == null || !FormatTask.STATUS_SUCCESS.equals(t.getStatus())) {
                enrichManual(c);
                continue;
            }
            c.setSourceType("real");
            User u = userMap.get(t.getUserId());
            c.setUsername(maskUsername(u == null ? null : u.getUsername(), c.getAuthor()));
            FormatTemplate tpl = tplMap.get(t.getTemplateId());
            c.setTemplateName(tpl == null ? null : tpl.getName());
            PaperFile f = fileMap.get(t.getFileId());
            c.setOriginalName(f == null ? null : f.getOriginalName());
            c.setMinutes(minutesOf(t));
            c.setHasDoc(t.getResultPath() != null);
            c.setPublicTemplateId(resolvePublicTemplateId(tpl));
        }
    }

    /** 手写案例: 仅派生试用入口(关联模板若公开) */
    private void enrichManual(SiteCase c) {
        c.setSourceType("manual");
        if (c.getTemplateId() != null) {
            FormatTemplate tpl = templateMapper.selectById(c.getTemplateId());
            if (tpl != null && Boolean.TRUE.equals(tpl.getIsPublic())) {
                c.setPublicTemplateId(tpl.getId());
            }
        }
    }

    private java.util.Map<Long, User> batchUsers(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids))
                .stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private java.util.Map<Long, FormatTemplate> batchTemplates(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>().in(FormatTemplate::getId, ids))
                .stream().collect(java.util.stream.Collectors.toMap(FormatTemplate::getId, t -> t, (a, b) -> a));
    }

    private java.util.Map<Long, PaperFile> batchFiles(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return paperFileMapper.selectList(new LambdaQueryWrapper<PaperFile>().in(PaperFile::getId, ids))
                .stream().collect(java.util.stream.Collectors.toMap(PaperFile::getId, f -> f, (a, b) -> a));
    }

    private Long minutesOf(FormatTask t) {
        if (t == null || t.getCreateTime() == null || t.getFinishTime() == null) {
            return null;
        }
        return java.time.Duration.between(t.getCreateTime(), t.getFinishTime()).toMinutes();
    }

    private String maskUsername(String username, String override) {
        if (StringUtils.hasText(override)) {
            return override;
        }
        if (!StringUtils.hasText(username)) {
            return "匿名用户";
        }
        return username.charAt(0) + "**";
    }

    private Long resolvePublicTemplateId(FormatTemplate tpl) {
        if (tpl == null) {
            return null;
        }
        if (Boolean.TRUE.equals(tpl.getIsPublic())) {
            return tpl.getId();
        }
        if (tpl.getSourceTemplateId() != null) {
            FormatTemplate src = templateMapper.selectById(tpl.getSourceTemplateId());
            if (src != null && Boolean.TRUE.equals(src.getIsPublic())) {
                return src.getId();
            }
        }
        return null;
    }

    public List<String> dictLabels(String dictType, List<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return dictDataMapper.selectList(new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getDictType, dictType).in(DictData::getDictValue, values))
                .stream().map(DictData::getDictLabel).collect(Collectors.toList());
    }
}
