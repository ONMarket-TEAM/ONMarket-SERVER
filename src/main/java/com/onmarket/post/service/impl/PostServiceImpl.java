package com.onmarket.post.service.impl;

import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailResponse;
import com.onmarket.post.dto.PostDetailWithScrapResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.post.service.PostService;
import com.onmarket.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CreditLoanProductRepository creditLoanProductRepository;
    private final LoanProductRepository loanProductRepository;
    private final ScrapService scrapService;

    @Override
    public List<PostListResponse> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return posts.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostListResponse> getPostsByType(PostType postType) {
        List<Post> posts = postRepository.findByPostTypeOrderByCreatedAtDesc(postType);
        return posts.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException());

        return convertToDetailResponse(post);
    }

    @Override
    @Transactional
    public void createPostsFromCreditLoanProducts() {
        // 페이징으로 처리하여 메모리 사용량 줄이기
        int pageSize = 50;
        int page = 0;

        Page<CreditLoanProduct> productPage;
        int totalProcessed = 0;
        int successCount = 0;

        do {
            Pageable pageable = PageRequest.of(page, pageSize);
            productPage = creditLoanProductRepository.findAll(pageable);

            for (CreditLoanProduct creditProduct : productPage.getContent()) {
                try {
                    // 별도 트랜잭션으로 각각 처리
                    processIndividualCreditProduct(creditProduct);
                    successCount++;
                } catch (Exception e) {
                    log.error("CreditLoanProduct({}) 처리 실패: {}", creditProduct.getId(), e.getMessage());
                }
                totalProcessed++;
            }

            page++;
            log.info("처리 진행률: {}/{}", totalProcessed, productPage.getTotalElements());

        } while (productPage.hasNext());

        log.info("신용대출 상품 Post 생성 완료 - 전체: {}, 성공: {}", totalProcessed, successCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualCreditProduct(CreditLoanProduct creditProduct) {
        // 중복 확인을 직접 쿼리로 수행
        Long count = postRepository.countBySourceTableAndSourceId("CreditLoanProduct", creditProduct.getId());

        if (count == 0) {
            Post post = createPostFromCreditLoanProduct(creditProduct);
            postRepository.save(post);
            log.debug("CreditLoanProduct({})에서 Post 생성 완료", creditProduct.getId());
        }
    }

    @Override
    @Transactional
    public void createPostsFromLoanProducts() {
        List<LoanProduct> loanProducts = loanProductRepository.findAll();

        for (LoanProduct loanProduct : loanProducts) {
            if (!postRepository.existsBySourceTableAndSourceId("LoanProduct", loanProduct.getId())) {
                Post post = createPostFromLoanProduct(loanProduct);
                postRepository.save(post);
                log.info("LoanProduct({})에서 Post({}) 생성 완료", loanProduct.getId(), post.getPostId());
            }
        }
    }

    private PostListResponse convertToListResponse(Post post) {
        return PostListResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .deadline(calculateDDay(post.getDeadline()))
                .productName(post.getProductName())
                .summary(post.getSummary())
                .build();
    }

    private PostDetailResponse convertToDetailResponse(Post post) {
        return PostDetailResponse.builder()
                .postId(post.getPostId())
                .productName(post.getProductName())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .joinLink(post.getJoinLink())
                .detailContent(post.getDetailContent())
                .build();
    }

    private Post createPostFromCreditLoanProduct(CreditLoanProduct creditProduct) {
        return Post.builder()
                .postType(PostType.LOAN)
                .productName(creditProduct.getFinPrdtNm())
                .summary("작성 예정")
                .deadline(creditProduct.getDclsEndDay())
                .companyName(creditProduct.getKorCoNm())
                .joinLink(creditProduct.getRltSite())
                .sourceTable("CreditLoanProduct")
                .sourceId(creditProduct.getId())
                .build();
    }

    private Post createPostFromLoanProduct(LoanProduct loanProduct) {
        return Post.builder()
                .postType(PostType.LOAN)
                .productName(loanProduct.getProductName())
                .summary("작성 예정")
                .companyName(loanProduct.getOfferingInstitution())
                .joinLink(loanProduct.getRelatedSite())
                .sourceTable("LoanProduct")
                .sourceId(loanProduct.getId())
                .build();
    }

    private String calculateDDay(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            return "상시 모집";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate deadline = LocalDate.parse(deadlineStr, formatter);
            LocalDate now = LocalDate.now();

            long daysBetween = ChronoUnit.DAYS.between(now, deadline);

            if (daysBetween > 0) {
                return "D-" + daysBetween;
            } else if (daysBetween == 0) {
                return "D-DAY";
            } else {
                return "마감";
            }
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", deadlineStr);
            return "상시";
        }
    }

    // === 새로 추가된 스크랩 관련 메서드 ===
    @Override
    public PostDetailWithScrapResponse getPostDetailWithScrap(Long postId, String email) {
        // 기본 게시물 정보 조회
        PostDetailResponse postDetail = getPostDetail(postId);

        // 스크랩 정보 조회
        boolean isScraped = false;
        if (email != null) {
            isScraped = scrapService.isScrapedByMe(email, postId);
        }
        Long scrapCount = scrapService.getScrapCount(postId);

        return PostDetailWithScrapResponse.from(postDetail, isScraped, scrapCount);
    }
    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException());
    }

}