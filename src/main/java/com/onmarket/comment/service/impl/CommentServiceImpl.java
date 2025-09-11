package com.onmarket.comment.service.impl;

import com.onmarket.comment.domain.Comment;
import com.onmarket.comment.dto.CommentCreateRequest;
import com.onmarket.comment.dto.CommentListResponse;
import com.onmarket.comment.dto.CommentResponse;
import com.onmarket.comment.dto.CommentUpdateRequest;
import com.onmarket.comment.exception.CommentAccessDeniedException;
import com.onmarket.comment.exception.CommentInvalidRatingException;
import com.onmarket.comment.exception.CommentNotFoundException;
import com.onmarket.comment.exception.CommentReplyDepthExceededException;
import com.onmarket.comment.exception.CommentReplyRatingNotAllowedException;
import com.onmarket.comment.exception.ParentCommentNotFoundException;
import com.onmarket.comment.repository.CommentRepository;
import com.onmarket.comment.service.CommentService;
import com.onmarket.member.domain.Member;
import com.onmarket.member.service.MemberService;
import com.onmarket.post.domain.Post;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberService memberService;

    @Override
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, String userEmail, String author) {
        // 게시물 존재 확인
        Post post = findPostById(request.getPostId());
        Member member = memberService.findByEmail(userEmail);

        Comment parentComment = null;

        // 대댓글인 경우 부모 댓글 확인
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .filter(comment -> !comment.getIsDeleted()) // 삭제되지 않은 댓글만
                    .orElseThrow(ParentCommentNotFoundException::new);

            // 부모 댓글이 같은 게시물에 속하는지 확인
            if (!parentComment.getPostId().equals(request.getPostId())) {
                throw new ParentCommentNotFoundException();
            }

            // 대댓글의 대댓글은 불가 (2단계까지만 허용)
            if (parentComment.getParentComment() != null) {
                throw new CommentReplyDepthExceededException();
            }

            // === 대댓글은 별점 없음 ===
            if (request.getRating() != null) {
                throw new CommentReplyRatingNotAllowedException();
            }
        }

        // === 별점 검증 (일반 댓글만) ===
        Integer rating = null;
        if (parentComment == null && request.getRating() != null) {
            // 일반 댓글이고 별점이 있는 경우
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new CommentInvalidRatingException();
            }
            rating = request.getRating();
        }

        Comment comment = Comment.builder()
                .post(post)
                .userEmail(userEmail)
                .author(member.getNickname())
                .content(request.getContent())
                .rating(rating) // 별점 추가
                .parentComment(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);

        log.info("댓글 작성 완료 - commentId: {}, postId: {}, userEmail: {}, rating: {}",
                savedComment.getCommentId(), request.getPostId(), userEmail, rating);

        return CommentResponse.from(savedComment, userEmail, memberService);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, String userEmail) {
        Comment comment = findCommentById(commentId);

        // 권한 확인
        if (!comment.isOwner(userEmail)) {
            throw new CommentAccessDeniedException();
        }

        // 삭제된 댓글은 수정 불가
        if (comment.getIsDeleted()) {
            throw new CommentNotFoundException();
        }

        // === 별점 처리 ===
        if (comment.isParentComment()) {
            // 일반 댓글인 경우 별점 수정 가능
            Integer rating = request.getRating();
            if (rating != null && (rating < 1 || rating > 5)) {
                throw new CommentInvalidRatingException();
            }
            comment.updateContentAndRating(request.getContent(), rating);
        } else {
            // 대댓글인 경우 별점 수정 불가
            if (request.getRating() != null) {
                throw new CommentReplyRatingNotAllowedException();
            }
            comment.updateContent(request.getContent());
        }

        Member member = memberService.findByEmail(comment.getUserEmail());
        log.info("댓글 수정 완료 - commentId: {}, userEmail: {}", commentId, userEmail);

        return CommentResponse.from(comment, userEmail, memberService);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        Comment comment = findCommentById(commentId);

        // 권한 확인
        if (!comment.isOwner(userEmail)) {
            throw new CommentAccessDeniedException();
        }

        // 이미 삭제된 댓글인지 확인
        if (comment.getIsDeleted()) {
            throw new CommentNotFoundException();
        }

        // 논리 삭제 수행
        comment.delete();

        // === 연쇄 삭제 추가 ===
        if (comment.isParentComment()) {
            // 부모 댓글인 경우 모든 자식 댓글도 삭제
            comment.getReplies().forEach(reply -> {
                if (!reply.getIsDeleted()) {
                    reply.delete();
                }
            });
        }

        log.info("댓글 삭제 완료 - commentId: {}, userEmail: {}", commentId, userEmail);
    }

    @Override
    public CommentListResponse getCommentsByPostId(Long postId, String currentUserEmail) {
        // 게시물 존재 확인
        findPostById(postId);

        List<Comment> parentComments = commentRepository.findParentCommentsByPostId(postId);

        Member currentMember = memberService.findByEmail(currentUserEmail);
        String currentNickname = currentMember.getNickname();

        List<CommentResponse> commentResponses = parentComments.stream()
                .map(comment -> {
                    // 이메일로 Member 조회
                    String commentUserEmail = comment.getUserEmail();
                    String nickname;
                    if (commentUserEmail.equals(currentUserEmail)) {
                        nickname = currentNickname;
                    } else {
                        nickname = memberService.findByEmail(commentUserEmail).getNickname();
                    }
                    return CommentResponse.from(comment, currentUserEmail, memberService);
                })
                .collect(Collectors.toList());

        return CommentListResponse.of(postId, commentResponses);
    }

    @Override
    public CommentResponse getComment(Long commentId, String currentUserEmail) {
        Comment comment = findCommentById(commentId);

        // 삭제된 댓글은 조회 불가
        if (comment.getIsDeleted()) {
            throw new CommentNotFoundException();
        }
        Member member = memberService.findByEmail(comment.getUserEmail());

        return CommentResponse.from(comment,currentUserEmail, memberService);
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }
}
