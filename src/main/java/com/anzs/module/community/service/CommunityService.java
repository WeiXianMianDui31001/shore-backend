package com.anzs.module.community.service;

import com.anzs.common.exception.BizException;
import com.anzs.infrastructure.cache.RedisCache;
import com.anzs.infrastructure.storage.AliOssService;
import com.anzs.module.community.dto.PostCreateDTO;
import com.anzs.module.community.entity.Comment;
import com.anzs.module.community.entity.Post;
import com.anzs.module.community.entity.PostCollect;
import com.anzs.module.community.entity.PostLike;
import com.anzs.module.community.mapper.CommentMapper;
import com.anzs.module.community.mapper.PostCollectMapper;
import com.anzs.module.community.mapper.PostEndorseMapper;
import com.anzs.module.community.mapper.PostLikeMapper;
import com.anzs.module.community.mapper.PostMapper;
import com.anzs.module.community.vo.PostVO;
import com.anzs.module.notification.service.NotificationService;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.user.mapper.SysUserMapper;
import com.anzs.module.user.service.PointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final PostEndorseMapper postEndorseMapper;
    private final SysUserMapper sysUserMapper;
    private final AliOssService aliOssService;
    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;
    private final PointsService pointsService;
    private final NotificationService notificationService;

    @Value("${shore.upload.path}")
    private String uploadPath;

    // ========== List & Search ==========

    public IPage<PostVO> list(String keyword, Integer page, Integer size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> records = postMapper.searchPosts(keyword, offset, size);
        long total = postMapper.countSearchPosts(keyword);

        List<PostVO> voList = records.stream()
                .map(p -> toPostVO(p, currentUserId))
                .collect(Collectors.toList());

        Page<PostVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(voList);
        resultPage.setTotal(total);
        return resultPage;
    }

    // ========== Detail ==========

    @Transactional
    public PostVO detail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        if (post.getStatus() != null && post.getStatus() == 2) {
            if (currentUserId == null || (!currentUserId.equals(post.getAuthorId()) && !isAdmin(currentUserId))) {
                throw new BizException("帖子不存在");
            }
        }
        incrementViewCount(postId, currentUserId);
        post.setViewCount(post.getViewCount() + 1);
        return toPostVO(post, currentUserId);
    }

    private void incrementViewCount(Long postId, Long userId) {
        if (userId != null) {
            String key = "post:view:" + postId + ":" + userId;
            if (!Boolean.TRUE.equals(redisCache.hasKey(key))) {
                postMapper.update(null, new LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .setSql("view_count = view_count + 1"));
                redisCache.set(key, "1", 5, TimeUnit.MINUTES);
            }
        } else {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("view_count = view_count + 1"));
        }
    }

    // ========== Create ==========

    @Transactional
    public void createPost(Long userId, PostCreateDTO dto) {
        Post post = new Post();
        post.setAuthorId(userId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setTags(dto.getTags());
        post.setImages(dto.getImages());
        post.setScene(dto.getScene());
        post.setStatus(0);
        post.setIsPinned(false);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);
    }

    // ========== Like / Collect ==========

    @Transactional
    public void likePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        long count = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        if (count > 0) return;
        PostLike pl = new PostLike();
        pl.setPostId(postId);
        pl.setUserId(userId);
        pl.setCreatedAt(LocalDateTime.now());
        postLikeMapper.insert(pl);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("like_count = like_count + 1"));
        if (!post.getAuthorId().equals(userId)) {
            sendInteractionNotification(userId, post.getAuthorId(), "点赞了你的帖子", post.getTitle(), postId, "POST_LIKE");
        }
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        long count = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        if (count == 0) return;
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("like_count = GREATEST(like_count - 1, 0)"));
    }

    @Transactional
    public void collectPost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        long count = postCollectMapper.selectCount(new LambdaQueryWrapper<PostCollect>()
                .eq(PostCollect::getPostId, postId)
                .eq(PostCollect::getUserId, userId));
        if (count > 0) return;
        PostCollect pc = new PostCollect();
        pc.setPostId(postId);
        pc.setUserId(userId);
        pc.setCreatedAt(LocalDateTime.now());
        postCollectMapper.insert(pc);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("collect_count = collect_count + 1"));
        if (!post.getAuthorId().equals(userId)) {
            sendInteractionNotification(userId, post.getAuthorId(), "收藏了你的帖子", post.getTitle(), postId, "POST_COLLECT");
        }
    }

    @Transactional
    public void uncollectPost(Long userId, Long postId) {
        long count = postCollectMapper.selectCount(new LambdaQueryWrapper<PostCollect>()
                .eq(PostCollect::getPostId, postId)
                .eq(PostCollect::getUserId, userId));
        if (count == 0) return;
        postCollectMapper.delete(new LambdaQueryWrapper<PostCollect>()
                .eq(PostCollect::getPostId, postId)
                .eq(PostCollect::getUserId, userId));
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("collect_count = GREATEST(collect_count - 1, 0)"));
    }

    // ========== Recommendation ==========

    public IPage<PostVO> myCollects(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Post> records = postMapper.selectUserCollectedPosts(userId, offset, size);
        long total = postMapper.countUserCollectedPosts(userId);
        List<PostVO> voList = records.stream()
                .map(p -> toPostVO(p, userId))
                .collect(Collectors.toList());
        Page<PostVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(voList);
        resultPage.setTotal(total);
        return resultPage;
    }

    public IPage<PostVO> recommendPosts(Long userId, Integer userRole, Integer page, Integer size) {
        Set<String> interestTags = buildInterestTags(userId);
        // 获取近90天的候选帖子
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        List<Post> candidates = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .in(Post::getStatus, 0, 1)
                        .ge(Post::getCreatedAt, since)
                        .orderByDesc(Post::getIsPinned)
                        .last("LIMIT 200")
        );

        LocalDateTime now = LocalDateTime.now();
        List<ScoredPost> scored = candidates.stream()
                .map(p -> new ScoredPost(p, computeScore(p, userRole, interestTags, now)))
                .sorted(Comparator.comparingDouble(ScoredPost::getScore).reversed())
                .collect(Collectors.toList());

        int total = scored.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<PostVO> records = (from >= total) ? Collections.emptyList() :
                scored.subList(from, to).stream()
                        .map(s -> toPostVO(s.getPost(), userId))
                        .collect(Collectors.toList());

        Page<PostVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(records);
        resultPage.setTotal(total);
        return resultPage;
    }

    @SneakyThrows
    private double computeScore(Post post, Integer userRole, Set<String> interestTags, LocalDateTime now) {
        long hours = Duration.between(post.getCreatedAt(), now).toHours();
        double timeDecay = Math.exp(-hours / 72.0);

        double interestMatch = 0.0;
        if (post.getTags() != null && !post.getTags().isEmpty() && !interestTags.isEmpty()) {
            try {
                List<String> postTags = objectMapper.readValue(post.getTags(), new TypeReference<List<String>>() {});
                long match = postTags.stream().filter(interestTags::contains).count();
                interestMatch = match * 0.5;
            } catch (Exception ignored) {}
        }

        double hotBoost = Math.log(1 + post.getLikeCount() + post.getCollectCount() + post.getViewCount() / 10.0 + 1.0);
        double sceneBonus = (post.getScene() != null && post.getScene().equals(userRole)) ? 1.5 : 1.0;
        double pinBonus = Boolean.TRUE.equals(post.getIsPinned()) ? 2.0 : 1.0;

        return timeDecay * (1 + interestMatch) * hotBoost * sceneBonus * pinBonus;
    }

    @SneakyThrows
    private Set<String> buildInterestTags(Long userId) {
        Set<String> tags = new HashSet<>();
        if (userId == null) return tags;
        List<String> likeTags = postMapper.findUserLikeTags(userId);
        List<String> collectTags = postMapper.findUserCollectTags(userId);
        for (String json : likeTags) {
            if (json != null && !json.isEmpty()) {
                try {
                    List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                    tags.addAll(list);
                } catch (Exception ignored) {}
            }
        }
        for (String json : collectTags) {
            if (json != null && !json.isEmpty()) {
                try {
                    List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                    tags.addAll(list);
                } catch (Exception ignored) {}
            }
        }
        return tags;
    }

    // ========== Image Upload ==========

    public Map<String, Object> uploadImage(org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + ext;
        java.nio.file.Path dir = java.nio.file.Paths.get(uploadPath, "community");
        try {
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
            }
            java.nio.file.Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());
        } catch (Exception e) {
            throw new BizException("图片保存失败: " + e.getMessage());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("url", "/uploads/community/" + fileName);
        return map;
    }

    // ========== Comment ==========

    public List<Comment> comments(Long postId) {
        return commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .in(Comment::getStatus, 0, 1)
                        .orderByDesc(Comment::getCreatedAt)
        );
    }

    @Transactional
    public void addComment(Long userId, Long postId, Long parentId, String content, String imagesJson) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        if (post.getStatus() != null && post.getStatus() == 2) {
            throw new BizException("帖子已下架，无法评论");
        }
        if ((content == null || content.trim().isEmpty()) && (imagesJson == null || imagesJson.isEmpty())) {
            throw new BizException("评论内容或图片至少填一项");
        }
        Comment c = new Comment();
        c.setPostId(postId);
        c.setParentId(parentId);
        c.setAuthorId(userId);
        c.setContent(content);
        c.setImages(imagesJson);
        c.setStatus(0);
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);

        if (!post.getAuthorId().equals(userId)) {
            sendInteractionNotification(userId, post.getAuthorId(), "评论了你的帖子", post.getTitle(), postId, "POST_COMMENT");
        }
    }

    @Transactional
    public void resolvePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getAuthorId().equals(userId)) {
            throw new BizException("无权限操作");
        }
        post.setStatus(1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    // ========== Admin operations ==========

    @Transactional
    public void updatePostStatus(Long postId, Integer status) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        post.setStatus(status);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    @Transactional
    public void updateCommentStatus(Long commentId, Integer status) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BizException("评论不存在");
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    @Transactional
    public void pinPost(Long postId, Boolean pinned) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        post.setIsPinned(pinned);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    public IPage<PostVO> adminList(String keyword, Integer status, Integer page, Integer size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(Post::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        qw.orderByDesc(Post::getCreatedAt);
        IPage<Post> postPage = postMapper.selectPage(p, qw);
        List<PostVO> voList = postPage.getRecords().stream()
                .map(post -> toPostVO(post, null))
                .collect(Collectors.toList());
        Page<PostVO> result = new Page<>(page, size);
        result.setRecords(voList);
        result.setTotal(postPage.getTotal());
        return result;
    }

    // ========== Endorsement / Excellence ==========

    @Transactional
    public void endorsePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        if (post.getAuthorId().equals(userId)) throw new BizException("不能认可自己的帖子");
        long count = postEndorseMapper.selectCount(new LambdaQueryWrapper<com.anzs.module.community.entity.PostEndorse>()
                .eq(com.anzs.module.community.entity.PostEndorse::getPostId, postId)
                .eq(com.anzs.module.community.entity.PostEndorse::getUserId, userId));
        if (count > 0) throw new BizException("已经认可过该帖子");

        com.anzs.module.community.entity.PostEndorse pe = new com.anzs.module.community.entity.PostEndorse();
        pe.setPostId(postId);
        pe.setUserId(userId);
        pe.setCreatedAt(LocalDateTime.now());
        postEndorseMapper.insert(pe);

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("endorse_count = endorse_count + 1"));

        sendInteractionNotification(userId, post.getAuthorId(), "认可了你的帖子", post.getTitle(), postId, "POST_ENDORSE");
    }

    @Transactional
    public void unendorsePost(Long userId, Long postId) {
        long count = postEndorseMapper.selectCount(new LambdaQueryWrapper<com.anzs.module.community.entity.PostEndorse>()
                .eq(com.anzs.module.community.entity.PostEndorse::getPostId, postId)
                .eq(com.anzs.module.community.entity.PostEndorse::getUserId, userId));
        if (count == 0) return;
        postEndorseMapper.delete(new LambdaQueryWrapper<com.anzs.module.community.entity.PostEndorse>()
                .eq(com.anzs.module.community.entity.PostEndorse::getPostId, postId)
                .eq(com.anzs.module.community.entity.PostEndorse::getUserId, userId));
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("endorse_count = GREATEST(endorse_count - 1, 0)"));
    }

    @Transactional
    public void excellentPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        post.setIsExcellent(true);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);

        // 奖励发帖人积分
        pointsService.addPoints(post.getAuthorId(), 20, "EXCELLENT_POST", postId, "帖子被加精为经验帖");

        notificationService.sendNotification(post.getAuthorId(), 1, "帖子被加精",
                "你的帖子《" + post.getTitle() + "》被加精为经验帖", postId, "POST_EXCELLENT");
    }

    // ========== Helper ==========

    private PostVO toPostVO(Post post, Long currentUserId) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setAuthorId(post.getAuthorId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setTags(post.getTags());
        vo.setImages(post.getImages());
        vo.setScene(post.getScene());
        vo.setStatus(post.getStatus());
        vo.setIsPinned(post.getIsPinned());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCollectCount(post.getCollectCount());
        vo.setEndorseCount(post.getEndorseCount());
        vo.setIsExcellent(post.getIsExcellent());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());

        SysUser author = sysUserMapper.selectById(post.getAuthorId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorAvatar(author.getAvatarUrl());
            vo.setAuthorRole(author.getRole());
        }

        if (currentUserId != null) {
            vo.setLiked(postMapper.countUserLike(post.getId(), currentUserId) > 0);
            vo.setCollected(postMapper.countUserCollect(post.getId(), currentUserId) > 0);
            vo.setEndorsed(postEndorseMapper.selectCount(new LambdaQueryWrapper<com.anzs.module.community.entity.PostEndorse>()
                    .eq(com.anzs.module.community.entity.PostEndorse::getPostId, post.getId())
                    .eq(com.anzs.module.community.entity.PostEndorse::getUserId, currentUserId)) > 0);
        } else {
            vo.setLiked(false);
            vo.setCollected(false);
            vo.setEndorsed(false);
        }
        return vo;
    }

    private void sendInteractionNotification(Long actorId, Long receiverId, String action, String postTitle, Long postId, String bizType) {
        SysUser actor = sysUserMapper.selectById(actorId);
        String nickname = actor != null ? actor.getNickname() : "有人";
        notificationService.sendNotification(receiverId, 1, "新的" + action.split("了")[0],
                nickname + action + "《" + postTitle + "》", postId, bizType);
    }

    private boolean isAdmin(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == 2;
    }

    @RequiredArgsConstructor
    private static class ScoredPost {
        private final Post post;
        private final double score;

        public Post getPost() { return post; }
        public double getScore() { return score; }
    }
}
