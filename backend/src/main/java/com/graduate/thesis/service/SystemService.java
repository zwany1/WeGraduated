package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.entity.Config;
import com.graduate.thesis.entity.DictData;
import com.graduate.thesis.entity.DictType;
import com.graduate.thesis.entity.Notice;
import com.graduate.thesis.mapper.ConfigMapper;
import com.graduate.thesis.mapper.DictDataMapper;
import com.graduate.thesis.mapper.DictTypeMapper;
import com.graduate.thesis.mapper.NoticeMapper;
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

    public SystemService(DictTypeMapper dictTypeMapper,
                         DictDataMapper dictDataMapper,
                         ConfigMapper configMapper,
                         NoticeMapper noticeMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
        this.configMapper = configMapper;
        this.noticeMapper = noticeMapper;
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

    public List<String> dictLabels(String dictType, List<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return dictDataMapper.selectList(new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getDictType, dictType).in(DictData::getDictValue, values))
                .stream().map(DictData::getDictLabel).collect(Collectors.toList());
    }
}
