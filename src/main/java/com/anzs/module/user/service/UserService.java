package com.anzs.module.user.service;

import com.anzs.module.user.entity.PointsTransaction;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.user.mapper.PointsTransactionMapper;
import com.anzs.module.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final PointsTransactionMapper pointsTransactionMapper;

    public Map<String, Object> getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("nickname", user.getNickname());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("role", user.getRole());
        map.put("pointsBalance", user.getPointsBalance());
        map.put("studentEmail", user.getStudentEmail());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    @Transactional
    public void updateProfile(Long userId, String nickname, String avatarUrl) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    public Map<String, Object> getPointsSummary(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        // 简单统计今日
        // 生产环境可改为按 created_at >= today 聚合
        Map<String, Object> map = new HashMap<>();
        map.put("balance", user.getPointsBalance());
        map.put("todayIncome", 0);
        map.put("todayExpense", 0);
        map.put("totalIncome", 0);
        map.put("totalExpense", 0);
        return map;
    }

    public IPage<PointsTransaction> getPointsTransactions(Long userId, Integer page, Integer size) {
        Page<PointsTransaction> p = new Page<>(page, size);
        return pointsTransactionMapper.selectPage(p,
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, userId)
                        .orderByDesc(PointsTransaction::getCreatedAt));
    }
}
