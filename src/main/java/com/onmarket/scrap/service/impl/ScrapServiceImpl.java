package com.onmarket.scrap.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.post.domain.Post;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.scrap.domain.Scrap;
import com.onmarket.scrap.dto.ScrapToggleResponse;
import com.onmarket.scrap.repository.ScrapRepository;
import com.onmarket.scrap.service.ScrapService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScrapServiceImpl implements ScrapService {

    private final ScrapRepository scrapRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional
    public ScrapToggleResponse toggleScrap(String email, Long postId) {
        Member member = findMemberByEmail(email);
        Post post = findPostById(postId);

        Optional<Scrap> existingScrap = scrapRepository.findByMemberAndPost(member, post);

        boolean isScraped;
        if (existingScrap.isPresent()) {
            // 스크랩 해제
            scrapRepository.delete(existingScrap.get());
            isScraped = false;
            log.info("사용자 {}가 게시물 {} 스크랩 해제", email, postId);
        } else {
            // 스크랩 추가
            Scrap newScrap = Scrap.create(member, post);
            scrapRepository.save(newScrap);
            isScraped = true;
            log.info("사용자 {}가 게시물 {} 스크랩 추가", email, postId);
        }

        // 최신 스크랩 개수 조회
        Long scrapCount = scrapRepository.countByPost(post);
        return ScrapToggleResponse.of(isScraped, scrapCount);
    }

    @Override
    public List<PostListResponse> getMyScraps(String email) {
        Member member = findMemberByEmail(email);

        return scrapRepository.findByMemberOrderByCreatedAtDesc(member)
                .stream()
                .map(this::convertToPostListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isScrapedByMe(Long memberId, Long postId) {
        // 성능 최적화: Entity 조회 없이 ID만으로 확인
        return scrapRepository.existsByMemberMemberIdAndPostPostId(memberId, postId);
    }

    @Override
    public Long getScrapCount(Long postId) {
        // 성능 최적화: Entity 조회 없이 ID만으로 카운트
        return scrapRepository.countByPostId(postId);
    }

    // === Private 헬퍼 메서드들 ===

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. Email: " + email));
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다. ID: " + postId));
    }

    private PostListResponse convertToPostListResponse(Scrap scrap) {
        Post post = scrap.getPost();
        return PostListResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .productName(post.getProductName())
                .summary(post.getSummary())
                .deadline(calculateDDay(post.getDeadline()))
                .build();
    }

    private String calculateDDay(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            return "상시 모집";
        }

        try {
            LocalDate deadline = LocalDate.parse(deadlineStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

            if (daysBetween > 0) return "D-" + daysBetween;
            else if (daysBetween == 0) return "D-DAY";
            else return "마감";
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", deadlineStr);
            return "상시";
        }
    }
}