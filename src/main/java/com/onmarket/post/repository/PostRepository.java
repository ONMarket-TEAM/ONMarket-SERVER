package com.onmarket.post.repository;

import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByPostTypeOrderByCreatedAtDesc(PostType postType, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.sourceTable = :sourceTable AND p.sourceId = :sourceId")
    Long countBySourceTableAndSourceId(@Param("sourceTable") String sourceTable, @Param("sourceId") Long sourceId);

    boolean existsBySourceTableAndSourceId(String sourceTable, Long sourceId);

    // 검색 기능
    @Query(value = "SELECT p FROM Post p WHERE p.postType = :postType AND " +
            "(:keyword IS NULL OR p.productName LIKE :keyword OR p.companyName LIKE :keyword) AND " +
            "(:companyName IS NULL OR p.companyName LIKE :companyName) " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.postType = :postType AND " +
                    "(:keyword IS NULL OR p.productName LIKE :keyword OR p.companyName LIKE :keyword) AND " +
                    "(:companyName IS NULL OR p.companyName LIKE :companyName)")
    Page<Post> searchPosts(@Param("postType") PostType postType,
                           @Param("keyword") String keyword,
                           @Param("companyName") String companyName,
                           Pageable pageable);
}