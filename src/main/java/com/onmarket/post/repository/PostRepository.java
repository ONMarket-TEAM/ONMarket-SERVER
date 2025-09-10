package com.onmarket.post.repository;

import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    Page<Post> findByPostType(PostType postType, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    Long countBySourceTableAndSourceId(@Param("sourceTable") String sourceTable, @Param("sourceId") Long sourceId);

    boolean existsBySourceTableAndSourceId(String sourceTable, Long sourceId);

    // ✅ 추가: source_table + source_id 로 summary/detail_content 바로 업데이트
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.summary = :summary, p.detailContent = :detailContent " +
            "WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    int updateSummaryBySource(@Param("sourceTable") String sourceTable,
                              @Param("sourceId") Long sourceId,
                              @Param("summary") String summary,
                              @Param("detailContent") String detailContent);
}
