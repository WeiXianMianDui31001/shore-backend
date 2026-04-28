package com.anzs.module.user.service;

import com.anzs.module.notification.service.NotificationService;
import com.anzs.module.user.entity.PointsRule;
import com.anzs.module.user.entity.PointsTransaction;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.user.mapper.PointsRuleMapper;
import com.anzs.module.user.mapper.PointsTransactionMapper;
import com.anzs.module.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final SysUserMapper sysUserMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final NotificationService notificationService;

    @Transactional
    public boolean addPoints(Long userId, Integer amount, String sourceType, Long bizId, String note) {
        if (amount == null || amount <= 0) return true;
        if (!checkDailyLimit(userId, amount)) {
            return false;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return false;

        int rows = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .setSql("points_balance = points_balance + " + amount)
                .set(SysUser::getUpdatedAt, LocalDateTime.now()));
        if (rows == 0) return false;

        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(userId);
        tx.setType(1);
        tx.setAmount(amount);
        tx.setBalanceAfter(user.getPointsBalance() + amount);
        tx.setSourceType(sourceType);
        tx.setBizId(bizId);
        tx.setCreatedAt(LocalDateTime.now());
        pointsTransactionMapper.insert(tx);

        notificationService.sendNotification(userId, 2, "积分到账",
                "+" + amount + "积分" + (note != null ? "：" + note : ""), bizId, sourceType);
        return true;
    }

    @Transactional
    public boolean deductPoints(Long userId, Integer amount, String sourceType, Long bizId, String note) {
        if (amount == null || amount <= 0) return true;
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getPointsBalance() < amount) return false;

        int rows = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .ge(SysUser::getPointsBalance, amount)
                .setSql("points_balance = points_balance - " + amount)
                .set(SysUser::getUpdatedAt, LocalDateTime.now()));
        if (rows == 0) return false;

        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(userId);
        tx.setType(2);
        tx.setAmount(-amount);
        tx.setBalanceAfter(user.getPointsBalance() - amount);
        tx.setSourceType(sourceType);
        tx.setBizId(bizId);
        tx.setCreatedAt(LocalDateTime.now());
        pointsTransactionMapper.insert(tx);

        notificationService.sendNotification(userId, 2, "积分支出",
                "-" + amount + "积分" + (note != null ? "：" + note : ""), bizId, sourceType);
        return true;
    }

    private boolean checkDailyLimit(Long userId, Integer amount) {
        PointsRule rule = pointsRuleMapper.selectCurrentActive();
        if (rule == null || rule.getDailyLimit() == null || rule.getDailyLimit() <= 0) return true;
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Integer todayEarned = pointsTransactionMapper.selectList(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, userId)
                        .eq(PointsTransaction::getType, 1)
                        .ge(PointsTransaction::getCreatedAt, todayStart)
        ).stream().mapToInt(PointsTransaction::getAmount).sum();
        return todayEarned + amount <= rule.getDailyLimit();
    }
}
