package com.onmarket.post.repository;

import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    // === 기존 메서드들 ===
    Page<Post> findByPostType(PostType postType, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    Long countBySourceTableAndSourceId(@Param("sourceTable") String sourceTable, @Param("sourceId") Long sourceId);

    boolean existsBySourceTableAndSourceId(String sourceTable, Long sourceId);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.summary = :summary, p.detailContent = :detailContent " +
            "WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    int updateSummaryBySource(@Param("sourceTable") String sourceTable,
                              @Param("sourceId") Long sourceId,
                              @Param("summary") String summary,
                              @Param("detailContent") String detailContent);

    @Query("SELECT p FROM Post p ORDER BY p.scrapCount DESC, p.createdAt DESC")
    List<Post> findTopByScrapCountOrderByScrapCountDesc(Pageable pageable);

    List<Post> findBySourceTableAndSourceIdIn(String sourceTable, Set<Long> sourceIds);

    List<Post> findBySourceTable(String sourceTable);

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findTop5ByOrderByCreatedAtDesc(Pageable pageable);

    default List<Post> findTop5ByOrderByCreatedAtDesc() {
        return findTop5ByOrderByCreatedAtDesc(PageRequest.of(0, 5));
    }

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    default List<Post> findTop10ByOrderByCreatedAtDesc() {
        return findTop10ByOrderByCreatedAtDesc(PageRequest.of(0, 10));
    }

    List<Post> findTop20ByOrderByCreatedAtDesc();

    // === 우선순위 추천 시스템을 위한 추가 메서드들 ===

    /**
     * 우선순위 추천을 위한 최신 게시물 50개 조회
     */
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findTop50ByOrderByCreatedAtDesc(Pageable pageable);

    default List<Post> findTop50ByOrderByCreatedAtDesc() {
        return findTop50ByOrderByCreatedAtDesc(PageRequest.of(0, 50));
    }

    /**
     * 타입별 최신 게시물 조회 (폴백용)
     */
    @Query("SELECT p FROM Post p WHERE p.postType = :postType ORDER BY p.createdAt DESC")
    List<Post> findTopByPostTypeOrderByCreatedAtDesc(@Param("postType") PostType postType, Pageable pageable);

    /**
     * Collection을 받는 sourceId 조회 메서드 (Set과 List 모두 지원)
     */
    List<Post> findBySourceTableAndSourceIdIn(String sourceTable, Collection<Long> sourceIds);

    /** source_table + source_id 매칭으로 image_url 갱신 */
    @Modifying
    @Transactional
    @Query("""
    UPDATE Post p
       SET p.imageUrl  = :url,
           p.updatedAt = :ts
     WHERE p.sourceTable = :sourceTable
       AND p.sourceId    = :sourceId
""")
    int updateImageUrlBySource(String sourceTable, Long sourceId, String url, LocalDateTime ts);
}