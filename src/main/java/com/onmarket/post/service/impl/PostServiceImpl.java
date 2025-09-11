package com.onmarket.post.service.impl;

import com.onmarket.cardnews.dto.TargetType;
import com.onmarket.cardnews.service.CardNewsService;
import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.fssdata.repository.CreditLoanProductRepository;
import com.onmarket.loandata.domain.LoanProduct;
import com.onmarket.loandata.repository.LoanProductRepository;
import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostSpecification;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.dto.PostDetailResponse;
import com.onmarket.post.dto.PostDetailWithScrapResponse;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.post.dto.PostSingleResponse;
import com.onmarket.post.exception.PostNotFoundException;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.post.service.PostService;
import com.onmarket.scrap.repository.ScrapRepository;
import com.onmarket.scrap.service.ScrapService;
import com.onmarket.summary.service.SummaryService;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final SupportProductRepository supportProductRepository;
    private final ScrapService scrapService;
    private final SummaryService summaryService;
    private final CardNewsService cardNewsService;
    private final ScrapRepository scrapRepository;


    @Override
    public Page<PostListResponse> getPostsByType(PostType postType, Pageable pageable) {
        Page<Post> posts = postRepository.findByPostType(postType, pageable);
        return posts.map(this::convertToListResponse);
    }

    @Override
    @Cacheable(value = "topScrapedPosts", key = "'top5'", unless = "#result.size() < 5")
    public List<PostListResponse> getTopScrapedPosts() {
        log.info("스크랩 수 상위 5개 게시물 조회 시작");
        Pageable pageable = PageRequest.of(0, 5);
        List<Post> topPosts = postRepository.findTopByScrapCountOrderByScrapCountDesc(pageable);

        List<PostListResponse> result = topPosts.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        log.info("스크랩 수 상위 게시물 조회 완료 - {}개", result.size());
        return result;
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException());
        return convertToDetailResponse(post);
    }

    @Override
    public PostSingleResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        return convertToSingleResponse(post);
    }

    @Override
    public PostDetailWithScrapResponse getPostDetailWithScrap(Long postId, String email) {
        PostDetailResponse postDetail = getPostDetail(postId);
        boolean isScraped = false;
        if (email != null) {
            isScraped = scrapService.isScrapedByMe(email, postId);
        }
        Long scrapCount = scrapService.getScrapCount(postId);
        return PostDetailWithScrapResponse.from(postDetail, isScraped, scrapCount);
    }

    @Override
    public Page<PostListResponse> searchPosts(PostType postType, String keyword, String companyName,
                                              Pageable pageable) {
        Specification<Post> spec = PostSpecification.search(postType, keyword, companyName);
        Page<Post> posts = postRepository.findAll(spec, pageable);
        return posts.map(this::convertToListResponse);
    }

    @Override
    @Transactional
    public void createPostsFromCreditLoanProducts() {
        log.info("신용대출 상품 데이터 동기화 시작 (테스트: 1개만 처리)...");
        Pageable pageRequest = PageRequest.of(1, 1, Sort.by("id").ascending());
        Page<CreditLoanProduct> productPage = creditLoanProductRepository.findAll(pageRequest);
        processProducts(productPage.getContent(), "신용대출 (테스트)", this::processIndividualCreditProduct);
    }

    @Override
    @Transactional
    public void createPostsFromLoanProducts() {
        log.info("일반대출 상품 데이터 동기화 시작 (테스트: 1개만 처리)...");
        // 🔥 [수정] 5 -> 1로 변경하고 정렬 추가
        Pageable pageRequest = PageRequest.of(0, 1, Sort.by("id").ascending());
        Page<LoanProduct> productPage = loanProductRepository.findAll(pageRequest);
        processProducts(productPage.getContent(), "일반대출 (테스트)", this::processIndividualLoanProduct);
    }

    @Override
    @Transactional
    public void createPostsFromSupportProducts() {
        log.info("공공지원금 상품 데이터 동기화 시작 (테스트: 1개만 처리)...");
        // 🔥 [수정] 5 -> 1로 변경하고 정렬 추가
        Pageable pageRequest = PageRequest.of(0, 1, Sort.by("id").ascending());
        Page<SupportProduct> productPage = supportProductRepository.findAll(pageRequest);
        processProducts(productPage.getContent(), "공공지원금 (테스트)", this::processIndividualSupportProduct);
    }

    private <T> void processProducts(List<T> products, String productType, ProductProcessor<T> processor) {
        for (T product : products) {
            try {
                processor.process(product);
            } catch (Exception e) {
                log.error("[{}] 개별 상품 처리 실패: {} - 원인: {}", productType, product.toString(), e.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface ProductProcessor<T> {
        void process(T product) throws Exception;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualCreditProduct(CreditLoanProduct creditProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("CreditLoanProduct", creditProduct.getId())) {
            Post post = createPostFromCreditLoanProduct(creditProduct);
            Post savedPost = postRepository.save(post);
            log.info("Post({}) 기본 정보 저장 완료", savedPost.getPostId());
            enrichPostWithAiContent(savedPost, TargetType.CREDIT_LOAN_PRODUCT, creditProduct.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualLoanProduct(LoanProduct loanProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("LoanProduct", loanProduct.getId())) {
            Post post = createPostFromLoanProduct(loanProduct);
            Post savedPost = postRepository.save(post);
            log.info("Post({}) 기본 정보 저장 완료", savedPost.getPostId());
            enrichPostWithAiContent(savedPost, TargetType.LOAN_PRODUCT, loanProduct.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualSupportProduct(SupportProduct supportProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("SupportProduct", supportProduct.getId())) {
            Post post = createPostFromSupportProduct(supportProduct);
            Post savedPost = postRepository.save(post);
            log.info("Post({}) 기본 정보 저장 완료", savedPost.getPostId());
            enrichPostWithAiContent(savedPost, TargetType.SUPPORT_PRODUCT, supportProduct.getId());
        }
    }

    private void enrichPostWithAiContent(Post post, TargetType type, Long sourceId) {
        boolean isUpdated = false;

        try {
            String cardNewsUrl = cardNewsService.buildFromDbAndPersist(type, sourceId.toString());
            post.setImageUrl(cardNewsUrl);
            isUpdated = true;
            log.info("Post({}) 카드뉴스 생성 및 URL 설정 완료", post.getPostId());
        } catch (Exception e) {
            log.warn("Post({}) 카드뉴스 생성 실패: {}", post.getPostId(), e.getMessage());
        }

        try {
            String[] summaryResult = generateSummary(type, sourceId);
            if (summaryResult != null && summaryResult.length >= 2) {
                post.setDetailContent(summaryResult[0]);
                post.setSummary(summaryResult[1]);
                isUpdated = true;
                log.info("Post({}) AI 요약 생성 및 설정 완료", post.getPostId());
            }
        } catch (Exception e) {
            log.warn("Post({}) AI 요약 생성 실패: {}", post.getPostId(), e.getMessage());
        }

        if (isUpdated) {
            postRepository.save(post);
            log.info("Post({}) AI 콘텐츠 최종 저장 완료", post.getPostId());
        }
    }

    private String[] generateSummary(TargetType type, Long sourceId) throws Exception {
        return switch (type) {
            case CREDIT_LOAN_PRODUCT -> summaryService.generateForCredit(sourceId);
            case LOAN_PRODUCT -> summaryService.generateForLoan(sourceId);
            case SUPPORT_PRODUCT -> summaryService.generateForSupport(sourceId);
        };
    }

    private Post createPostFromCreditLoanProduct(CreditLoanProduct creditProduct) {
        return Post.builder()
                .postType(PostType.LOAN)
                .productName(creditProduct.getFinPrdtNm())
                .summary("요약 정보 생성 중...")
                .detailContent("상세 정보 생성 중...")
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
                .summary("요약 정보 생성 중...")
                .detailContent("상세 정보 생성 중...")
                .companyName(loanProduct.getOfferingInstitution())
                .joinLink(loanProduct.getRelatedSite())
                .sourceTable("LoanProduct")
                .sourceId(loanProduct.getId())
                .build();
    }

    private Post createPostFromSupportProduct(SupportProduct supportProduct) {
        return Post.builder()
                .postType(PostType.SUPPORT)
                .productName(supportProduct.getServiceName())
                .summary("요약 정보 생성 중...")
                .detailContent("상세 정보 생성 중...")
                .deadline(supportProduct.getEndDay())
                .companyName(supportProduct.getDepartmentName())
                .joinLink(supportProduct.getOnlineApplicationUrl() != null ?
                        supportProduct.getOnlineApplicationUrl() :
                        supportProduct.getDetailUrl())
                .sourceTable("SupportProduct")
                .sourceId(supportProduct.getId())
                .build();
    }

    private PostListResponse convertToListResponse(Post post) {
        return PostListResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .companyName(post.getCompanyName())
                .deadline(calculateDDay(post.getDeadline()))
                .productName(post.getProductName())
                .summary(post.getSummary())
                .build();
    }

    private PostSingleResponse convertToSingleResponse(Post post) {
        return PostSingleResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .companyName(post.getCompanyName())
                .deadline(calculateDDay(post.getDeadline()))
                .productName(post.getProductName())
                .summary(post.getSummary())
                .imageUrl(post.getImageUrl())
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
}