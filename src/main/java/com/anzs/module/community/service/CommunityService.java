package com.anzs.module.community.service;

import com.anzs.common.exception.BizException;
import com.anzs.module.community.entity.Comment;
import com.anzs.module.community.entity.Post;
import com.anzs.module.community.mapper.CommentMapper;
import com.anzs.module.community.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    public IPage<Post> list(String keyword, Integer page, Integer size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<>();
        qw.in(Post::getStatus, 0, 1);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        qw.orderByDesc(Post::getIsPinned).orderByDesc(Post::getCreatedAt);
        return postMapper.selectPage(p, qw);
    }

    public Post detail(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BizException("帖子不存在");
        return post;
    }

    @Transactional
    public void createPost(Long userId, String title, String content, String tagsJson) {
        Post post = new Post();
        post.setAuthorId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setTags(tagsJson);
        post.setStatus(0);
        post.setIsPinned(false);
        post.setViewCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);
    }

    public List<Comment> comments(Long postId) {
        return commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, 0)
                        .orderByDesc(Comment::getCreatedAt)
        );
    }

    @Transactional
    public void addComment(Long userId, Long postId, Long parentId, String content) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException("帖子不存在");
        Comment c = new Comment();
        c.setPostId(postId);
        c.setParentId(parentId);
        c.setAuthorId(userId);
        c.setContent(content);
        c.setStatus(0);
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);
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
}
