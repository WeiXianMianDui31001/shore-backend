package com.anzs.module.info.service;

import com.anzs.module.info.entity.InfoEntry;
import com.anzs.module.info.mapper.InfoEntryMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InfoService {

    private final InfoEntryMapper infoEntryMapper;

    public IPage<InfoEntry> list(Integer scene, String category, String keyword, Integer page, Integer size) {
        long total = infoEntryMapper.countSearchInfo(scene, category, keyword);
        List<InfoEntry> records = infoEntryMapper.searchInfo(scene, category, keyword, (page - 1) * size, size);
        Page<InfoEntry> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    public InfoEntry detail(Long id) {
        return infoEntryMapper.selectById(id);
    }

    public List<String> categories(Integer scene) {
        return infoEntryMapper.selectCategoriesByScene(scene);
    }
}
