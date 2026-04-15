package com.anzs.module.notification.service;

import com.anzs.module.notification.entity.Notification;
import com.anzs.module.notification.mapper.NotificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public Map<String, Object> list(Long userId, Integer type, Integer isRead, Integer page, Integer size) {
        Page<Notification> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId);
        if (type != null) qw.eq(Notification::getType, type);
        if (isRead != null) qw.eq(Notification::getIsRead, isRead == 1);
        qw.orderByDesc(Notification::getCreatedAt);
        IPage<Notification> result = notificationMapper.selectPage(p, qw);

        long unreadCount = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)
        );

        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("unreadCount", unreadCount);
        map.put("pagination", Map.of(
                "page", result.getCurrent(),
                "size", result.getSize(),
                "total", result.getTotal()
        ));
        return map;
    }

    @Transactional
    public void read(Long userId, List<Long> ids) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .in(Notification::getId, ids)
                .set(Notification::getIsRead, true));
    }

    @Transactional
    public void readAll(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true));
    }

    @Transactional
    public void sendNotification(Long userId, Integer type, String title, String content, Long bizId, String bizType) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setBizId(bizId);
        n.setBizType(bizType);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }
}
