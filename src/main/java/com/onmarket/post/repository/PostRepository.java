package com.onmarket.post.repository;

import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByPostTypeOrderByCreatedAtDesc(PostType postType);
    Optional<Post> findByPostId(Long postId);
}
