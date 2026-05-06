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
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        int todayIncome = pointsTransactionMapper.selectList(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, userId)
                        .eq(PointsTransaction::getType, 1)
                        .ge(PointsTransaction::getCreatedAt, todayStart))
                .stream().mapToInt(PointsTransaction::getAmount).sum();

        int todayExpense = Math.abs(pointsTransactionMapper.selectList(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, userId)
                        .eq(PointsTransaction::getType, 2)
                        .ge(PointsTransaction::getCreatedAt, todayStart))
                .stream().mapToInt(PointsTransaction::getAmount).sum());

        int totalIncome = pointsTransactionMapper.selectList(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, userId)
                        .eq(PointsTransaction::getType, 1))
                .stream().mapToInt(PointsTransaction::getAmount).sum();

        int totalExpense = Math.abs(pointsTransactionMapper.selectList(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, userId)
                        .eq(PointsTransaction::getType, 2))
                .stream().mapToInt(PointsTransaction::getAmount).sum());

        Map<String, Object> map = new HashMap<>();
        map.put("balance", user.getPointsBalance());
        map.put("todayIncome", todayIncome);
        map.put("todayExpense", todayExpense);
        map.put("totalIncome", totalIncome);
        map.put("totalExpense", totalExpense);
        return map;
    }

    public IPage<PointsTransaction> getPointsTransactions(Long userId, Integer page, Integer size, String sourceType) {
        Page<PointsTransaction> p = new Page<>(page, size);
        LambdaQueryWrapper<PointsTransaction> qw = new LambdaQueryWrapper<>();
        qw.eq(PointsTransaction::getUserId, userId);
        if (sourceType != null && !sourceType.isEmpty()) {
            qw.eq(PointsTransaction::getSourceType, sourceType);
        }
        qw.orderByDesc(PointsTransaction::getCreatedAt);
        return pointsTransactionMapper.selectPage(p, qw);
    }
}
