package com.onmarket.comment.repository;

import com.onmarket.comment.domain.Comment;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시물의 모든 부모 댓글 조회 (대댓글은 fetch join으로 함께 조회)
     * 삭제되지 않은 댓글만 조회, 최신순 정렬
     */
    @Query("SELECT DISTINCT c FROM Comment c " +
            "LEFT JOIN FETCH c.replies r " +
            "WHERE c.post.postId = :postId AND c.parentComment IS NULL AND c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findParentCommentsByPostId(@Param("postId") Long postId);
}