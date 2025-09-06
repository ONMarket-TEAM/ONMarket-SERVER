package com.onmarket.post.repository;

import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 전체 게시물 목록 조회 (최신순)
    List<Post> findAllByOrderByCreatedAtDesc();

    // 타입별 게시물 목록 조회 (최신순)
    List<Post> findByPostTypeOrderByCreatedAtDesc(PostType postType);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    Long countBySourceTableAndSourceId(@Param("sourceTable") String sourceTable, @Param("sourceId") Long sourceId);

    // 원본 데이터 존재 여부 확인
    boolean existsBySourceTableAndSourceId(String sourceTable, Long sourceId);
}