package com.anzs.module.info.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anzs.module.info.entity.InfoEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InfoEntryMapper extends BaseMapper<InfoEntry> {

    @Select("SELECT DISTINCT category FROM info_entry WHERE scene = #{scene} AND status = 0 AND category IS NOT NULL AND category != '' ORDER BY category")
    List<String> selectCategoriesByScene(@Param("scene") Integer scene);

    @Select("<script>" +
            "SELECT * FROM info_entry WHERE status = 0 " +
            "<if test='scene != null'>AND scene = #{scene}</if>" +
            "<if test='category != null and category.length() > 0'>AND category = #{category}</if>" +
            "<if test='keyword != null and keyword.length() > 0'>" +
            "AND to_tsvector('simple', title) @@ plainto_tsquery('simple', #{keyword})" +
            "</if>" +
            "ORDER BY sort_order DESC, updated_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<InfoEntry> searchInfo(@Param("scene") Integer scene, @Param("category") String category, @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM info_entry WHERE status = 0 " +
            "<if test='scene != null'>AND scene = #{scene}</if>" +
            "<if test='category != null and category.length() > 0'>AND category = #{category}</if>" +
            "<if test='keyword != null and keyword.length() > 0'>" +
            "AND to_tsvector('simple', title) @@ plainto_tsquery('simple', #{keyword})" +
            "</if>" +
            "</script>")
    long countSearchInfo(@Param("scene") Integer scene, @Param("category") String category, @Param("keyword") String keyword);
}
