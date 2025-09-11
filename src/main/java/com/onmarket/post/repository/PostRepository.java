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

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    Page<Post> findByPostType(PostType postType, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    Long countBySourceTableAndSourceId(@Param("sourceTable") String sourceTable, @Param("sourceId") Long sourceId);

    boolean existsBySourceTableAndSourceId(String sourceTable, Long sourceId);

    Optional<Post> findBySourceTableAndSourceId(String sourceTable, Long sourceId);
}
