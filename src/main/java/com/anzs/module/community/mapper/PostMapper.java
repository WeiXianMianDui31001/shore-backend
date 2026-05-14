package com.anzs.module.community.mapper;

import com.anzs.module.community.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("<script>" +
            "SELECT * FROM post WHERE status IN (0,1) " +
            "<if test='keyword != null and keyword.length() > 0'>" +
            "AND (title ILIKE '%' || #{keyword} || '%' OR content ILIKE '%' || #{keyword} || '%')" +
            "</if>" +
            "ORDER BY is_pinned DESC, created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Post> searchPosts(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM post WHERE status IN (0,1) " +
            "<if test='keyword != null and keyword.length() > 0'>" +
            "AND (title ILIKE '%' || #{keyword} || '%' OR content ILIKE '%' || #{keyword} || '%')" +
            "</if>" +
            "</script>")
    long countSearchPosts(@Param("keyword") String keyword);

    @Select("SELECT p.tags FROM post p JOIN post_like pl ON pl.post_id = p.id " +
            "WHERE pl.user_id = #{userId} AND p.tags IS NOT NULL LIMIT 50")
    List<String> findUserLikeTags(@Param("userId") Long userId);

    @Select("SELECT p.tags FROM post p JOIN post_collect pc ON pc.post_id = p.id " +
            "WHERE pc.user_id = #{userId} AND p.tags IS NOT NULL LIMIT 50")
    List<String> findUserCollectTags(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int countUserLike(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_collect WHERE post_id = #{postId} AND user_id = #{userId}")
    int countUserCollect(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT p.* FROM post p JOIN post_collect pc ON pc.post_id = p.id " +
            "WHERE pc.user_id = #{userId} AND p.status IN (0,1) " +
            "ORDER BY pc.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Post> selectUserCollectedPosts(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM post p JOIN post_collect pc ON pc.post_id = p.id " +
            "WHERE pc.user_id = #{userId} AND p.status IN (0,1)")
    long countUserCollectedPosts(@Param("userId") Long userId);
}
