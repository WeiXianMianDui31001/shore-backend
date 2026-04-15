package com.anzs.module.info.service;

import com.anzs.module.info.entity.InfoEntry;
import com.anzs.module.info.mapper.InfoEntryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InfoService {

    private final InfoEntryMapper infoEntryMapper;

    public IPage<InfoEntry> list(Integer scene, String category, String keyword, Integer page, Integer size) {
        Page<InfoEntry> p = new Page<>(page, size);
        LambdaQueryWrapper<InfoEntry> qw = new LambdaQueryWrapper<>();
        qw.eq(InfoEntry::getStatus, 0);
        if (scene != null) {
            qw.eq(InfoEntry::getScene, scene);
        }
        if (category != null && !category.isEmpty()) {
            qw.eq(InfoEntry::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(InfoEntry::getTitle, keyword));
        }
        qw.orderByDesc(InfoEntry::getSortOrder).orderByDesc(InfoEntry::getUpdatedAt);
        return infoEntryMapper.selectPage(p, qw);
    }

    public InfoEntry detail(Long id) {
        return infoEntryMapper.selectById(id);
    }
}
