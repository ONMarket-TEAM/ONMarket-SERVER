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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
        return topPosts.stream()
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
        log.info("신용대출 상품 전체 데이터 동기화 시작...");
        processProductsInPages(
                creditLoanProductRepository::findAll,
                "신용대출",
                this::processIndividualCreditProduct
        );
    }

    @Override
    @Transactional
    public void createPostsFromLoanProducts() {
        log.info("일반대출 상품 전체 데이터 동기화 시작...");
        processProductsInPages(
                loanProductRepository::findAll,
                "일반대출",
                this::processIndividualLoanProduct
        );
    }

    @Override
    @Transactional
    public void createPostsFromSupportProducts() {
        log.info("공공지원금 상품 전체 데이터 동기화 시작...");
        processProductsInPages(
                supportProductRepository::findAll,
                "공공지원금",
                this::processIndividualSupportProduct
        );
    }

    @Override
    @Transactional
    public void synchronizeModifiedPosts() {
        LocalDateTime lastSyncTime = postRepository.findTopByOrderByUpdatedAtDesc()
                .map(Post::getUpdatedAt)
                .orElse(LocalDateTime.of(2000, 1, 1, 0, 0));

        log.info("[스케줄러 동기화 시작] 기준 시간: {}", lastSyncTime);

        List<CreditLoanProduct> modifiedCreditLoans = creditLoanProductRepository.findByUpdatedAtAfter(lastSyncTime);
        modifiedCreditLoans.forEach(this::createOrUpdatePost);
        log.info("신용대출 상품 {}건 동기화 완료", modifiedCreditLoans.size());

        List<LoanProduct> modifiedLoans = loanProductRepository.findByUpdatedAtAfter(lastSyncTime);
        modifiedLoans.forEach(this::createOrUpdatePost);
        log.info("일반대출 상품 {}건 동기화 완료", modifiedLoans.size());

        List<SupportProduct> modifiedSupports = supportProductRepository.findByUpdatedAtAfter(lastSyncTime);
        modifiedSupports.forEach(this::createOrUpdatePost);
        log.info("공공지원금 상품 {}건 동기화 완료", modifiedSupports.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrUpdatePost(CreditLoanProduct creditProduct) {
        Post post = postRepository.findBySourceTableAndSourceId("CreditLoanProduct", creditProduct.getId())
                .orElseGet(() -> createPostFromCreditLoanProduct(creditProduct));

        updatePostFrom(post, creditProduct);
        enrichPostWithAiContent(post, TargetType.CREDIT_LOAN_PRODUCT, creditProduct.getId());
        postRepository.save(post);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrUpdatePost(LoanProduct loanProduct) {
        Post post = postRepository.findBySourceTableAndSourceId("LoanProduct", loanProduct.getId())
                .orElseGet(() -> createPostFromLoanProduct(loanProduct));
        updatePostFrom(post, loanProduct);
        enrichPostWithAiContent(post, TargetType.LOAN_PRODUCT, loanProduct.getId());
        postRepository.save(post);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrUpdatePost(SupportProduct supportProduct) {
        Post post = postRepository.findBySourceTableAndSourceId("SupportProduct", supportProduct.getId())
                .orElseGet(() -> createPostFromSupportProduct(supportProduct));
        updatePostFrom(post, supportProduct);
        enrichPostWithAiContent(post, TargetType.SUPPORT_PRODUCT, supportProduct.getId());
        postRepository.save(post);
    }

    private <T> void processProductsInPages(PageableFunction<T> pagedFinder, String productType, ProductProcessor<T> processor) {
        int page = 0;
        final int PAGE_SIZE = 100;
        Page<T> productPage;
        do {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
            productPage = pagedFinder.apply(pageable);
            log.info("[{}] 처리 진행: {}/{} 페이지 (현재 페이지 항목: {}개)",
                    productType, page + 1, productPage.getTotalPages(), productPage.getNumberOfElements());
            processProducts(productPage.getContent(), productType, processor);
            page++;
        } while (productPage.hasNext());
        log.info("{} 상품 Post 생성 전체 완료 - 총 {}개 페이지 처리 완료", productType, productPage.getTotalPages());
    }

    @FunctionalInterface
    private interface PageableFunction<T> {
        Page<T> apply(Pageable pageable);
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
            // 🔥 save 전에 필수 값들을 먼저 채워줍니다.
            updatePostFrom(post, creditProduct);
            enrichPostWithAiContent(post, TargetType.CREDIT_LOAN_PRODUCT, creditProduct.getId());
            // 🔥 모든 내용이 채워진 후 마지막에 한 번만 저장합니다.
            postRepository.save(post);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualLoanProduct(LoanProduct loanProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("LoanProduct", loanProduct.getId())) {
            Post post = createPostFromLoanProduct(loanProduct);
            updatePostFrom(post, loanProduct);
            enrichPostWithAiContent(post, TargetType.LOAN_PRODUCT, loanProduct.getId());
            postRepository.save(post);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualSupportProduct(SupportProduct supportProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("SupportProduct", supportProduct.getId())) {
            Post post = createPostFromSupportProduct(supportProduct);
            updatePostFrom(post, supportProduct);
            enrichPostWithAiContent(post, TargetType.SUPPORT_PRODUCT, supportProduct.getId());
            postRepository.save(post);
        }
    }

    private void enrichPostWithAiContent(Post post, TargetType type, Long sourceId) {
        try {
            String cardNewsUrl = cardNewsService.buildFromDbAndPersist(type, sourceId.toString());
            post.setImageUrl(cardNewsUrl);
            log.info("Post(sourceId={}) 카드뉴스 URL 설정 완료", sourceId);
        } catch (Exception e) {
            log.warn("Post(sourceId={}) 카드뉴스 생성 실패: {}", sourceId, e.getMessage());
        }
        try {
            String[] summaryResult = generateSummary(type, sourceId);
            if (summaryResult != null && summaryResult.length >= 2) {
                post.setDetailContent(summaryResult[0]);
                post.setSummary(summaryResult[1]);
                log.info("Post(sourceId={}) AI 요약 설정 완료", sourceId);
            }
        } catch (Exception e) {
            log.warn("Post(sourceId={}) AI 요약 생성 실패: {}", sourceId, e.getMessage());
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
                .sourceTable("CreditLoanProduct")
                .sourceId(creditProduct.getId())
                .build();
    }

    private Post createPostFromLoanProduct(LoanProduct loanProduct) {
        return Post.builder()
                .postType(PostType.LOAN)
                .sourceTable("LoanProduct")
                .sourceId(loanProduct.getId())
                .build();
    }

    private Post createPostFromSupportProduct(SupportProduct supportProduct) {
        return Post.builder()
                .postType(PostType.SUPPORT)
                .sourceTable("SupportProduct")
                .sourceId(supportProduct.getId())
                .build();
    }

    private void updatePostFrom(Post post, CreditLoanProduct creditProduct) {
        post.setProductName(creditProduct.getFinPrdtNm());
        post.setCompanyName(creditProduct.getKorCoNm());
        post.setDeadline(creditProduct.getDclsEndDay());
        post.setJoinLink(creditProduct.getRltSite());
    }

    private void updatePostFrom(Post post, LoanProduct loanProduct) {
        post.setProductName(loanProduct.getProductName());
        post.setCompanyName(loanProduct.getOfferingInstitution());
        post.setJoinLink(loanProduct.getRelatedSite());
    }

    private void updatePostFrom(Post post, SupportProduct supportProduct) {
        post.setProductName(supportProduct.getServiceName());
        post.setDeadline(supportProduct.getEndDay());
        post.setCompanyName(supportProduct.getDepartmentName());
        post.setJoinLink(supportProduct.getOnlineApplicationUrl() != null ?
                supportProduct.getOnlineApplicationUrl() : supportProduct.getDetailUrl());
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