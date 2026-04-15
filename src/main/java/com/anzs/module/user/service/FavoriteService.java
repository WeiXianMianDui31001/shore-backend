package com.anzs.module.user.service;

import com.anzs.module.user.entity.UserFavorite;
import com.anzs.module.user.mapper.UserFavoriteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;

    @Transactional
    public void addFavorite(Long userId, String targetType, Long targetId) {
        UserFavorite exist = userFavoriteMapper.selectOne(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetType, targetType)
                        .eq(UserFavorite::getTargetId, targetId)
        );
        if (exist == null) {
            UserFavorite fav = new UserFavorite();
            fav.setUserId(userId);
            fav.setTargetType(targetType);
            fav.setTargetId(targetId);
            fav.setCreatedAt(LocalDateTime.now());
            userFavoriteMapper.insert(fav);
        }
    }

    @Transactional
    public void removeFavorite(Long userId, String targetType, Long targetId) {
        userFavoriteMapper.delete(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetType, targetType)
                        .eq(UserFavorite::getTargetId, targetId)
        );
    }

    public IPage<UserFavorite> listFavorites(Long userId, String targetType, Integer page, Integer size) {
        Page<UserFavorite> p = new Page<>(page, size);
        LambdaQueryWrapper<UserFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavorite::getUserId, userId);
        if (targetType != null && !targetType.isEmpty()) {
            qw.eq(UserFavorite::getTargetType, targetType);
        }
        qw.orderByDesc(UserFavorite::getCreatedAt);
        return userFavoriteMapper.selectPage(p, qw);
    }

    public boolean isFavorited(Long userId, String targetType, Long targetId) {
        return userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetType, targetType)
                        .eq(UserFavorite::getTargetId, targetId)
        ) > 0;
    }
}
