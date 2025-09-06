package com.onmarket.comment.domain;

import com.onmarket.common.domain.BaseTimeEntity;
import com.onmarket.post.domain.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    // Post 엔티티 참조 - CASCADE 옵션 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_COMMENT_POST",
                    foreignKeyDefinition = "FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE"))
    private Post post;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = true)
    private Integer rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id",
            foreignKey = @ForeignKey(name = "FK_COMMENT_PARENT",
                    foreignKeyDefinition = "FOREIGN KEY (parent_comment_id) REFERENCES comment(comment_id) ON DELETE CASCADE"))
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateContentAndRating(String content, Integer rating) {
        this.content = content;
        this.rating = rating;
    }

    public void delete() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다.";
    }

    public boolean isOwner(String email) {
        return userEmail.equals(email);
    }

    public boolean isParentComment() {
        return parentComment == null;
    }

    public Long getPostId() {
        return post != null ? post.getPostId() : null;
    }
}