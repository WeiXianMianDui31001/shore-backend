package com.anzs.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anzs.module.user.entity.PointsRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsRuleMapper extends BaseMapper<PointsRule> {
    @Select("SELECT * FROM points_rule WHERE is_active = true ORDER BY effective_time DESC LIMIT 1")
    PointsRule selectCurrentActive();
}
