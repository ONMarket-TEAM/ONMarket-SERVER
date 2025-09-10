package com.onmarket.scrap.service.impl;

import com.onmarket.member.domain.Member;
import com.onmarket.member.exception.MemberNotFountException;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.MemberService;
import com.onmarket.post.domain.Post;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.scrap.domain.Scrap;
import com.onmarket.scrap.dto.ScrapToggleResponse;
import com.onmarket.scrap.repository.ScrapRepository;
import com.onmarket.scrap.service.ScrapService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final MemberService memberService;

    @Override
    @Transactional
    public ScrapToggleResponse toggleScrap(String email, Long postId) {
        Member member = findMemberByEmail(email);
        Post post = findPostById(postId); // Post 엔티티를 여기서 조회합니다.

        Optional<Scrap> existingScrap = scrapRepository.findByMemberAndPost(member, post);

        boolean isScraped;
        if (existingScrap.isPresent()) {
            // 스크랩 해제
            scrapRepository.delete(existingScrap.get());
            post.decreaseScrapCount(); // 🔥 [핵심 수정 1] Post의 스크랩 카운트 1 감소
            isScraped = false;
            log.info("사용자 {}가 게시물 {} 스크랩 해제. 현재 스크랩 수: {}", email, postId, post.getScrapCount());
        } else {
            // 스크랩 추가
            Scrap newScrap = Scrap.create(member, post);
            scrapRepository.save(newScrap);
            post.increaseScrapCount(); // 🔥 [핵심 수정 2] Post의 스크랩 카운트 1 증가
            isScraped = true;
            log.info("사용자 {}가 게시물 {} 스크랩 추가. 현재 스크랩 수: {}", email, postId, post.getScrapCount());
        }

        // 🔥 [핵심 수정 3] DB를 다시 조회하는 대신, 이미 업데이트된 Post 객체의 카운트를 사용합니다.
        long scrapCount = post.getScrapCount();
        return ScrapToggleResponse.of(isScraped, scrapCount);
    }

    @Override
    public List<PostListResponse> getMyScraps(String email) {
        Member member = findMemberByEmail(email);

        return scrapRepository.findByMemberOrderByDeadlineAndCreatedAt(member, LocalDate.now(), Pageable.unpaged())
                .stream()
                .map(this::convertToPostListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isScrapedByMe(String email, Long postId) {
        Member member = memberService.findByEmail(email);
        return scrapRepository.existsByMemberMemberIdAndPostPostId(member.getMemberId(), postId);
    }

    @Override
    public Long getScrapCount(Long postId) {
        // 🔥 [최적화] Scrap 테이블을 전부 세는 대신, Post 엔티티의 scrapCount 값을 직접 반환합니다.
        Post post = findPostById(postId);
        return (long) post.getScrapCount();
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFountException());
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException());
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

        String s = deadlineStr.trim();
        if ("99991231".equals(s)) {
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