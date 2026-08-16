package com.graduate.thesis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduate.thesis.entity.LoginSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginSessionMapper extends BaseMapper<LoginSession> {
}
