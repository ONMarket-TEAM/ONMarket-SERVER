package com.onmarket.post.service.impl;

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
import com.onmarket.scrap.service.ScrapService;
import com.onmarket.summary.service.SummaryService;
import com.onmarket.supportsdata.domain.SupportProduct;
import com.onmarket.supportsdata.repository.SupportProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    @Override
    public Page<PostListResponse> getPostsByType(PostType postType, Pageable pageable) {
        Page<Post> posts = postRepository.findByPostType(postType, pageable);
        return posts.map(this::convertToListResponse);
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

    // 🔥 =================================================================
    // 🔥 [핵심 수정] 테스트를 위해 5개만 처리하도록 변경된 메서드들
    // 🔥 =================================================================

    @Override
    @Transactional
    public void createPostsFromCreditLoanProducts() {
        log.info("신용대출 상품 데이터 동기화 시작 (테스트: 5개만 처리)...");
        Pageable pageRequest = PageRequest.of(0, 5);
        Page<CreditLoanProduct> productPage = creditLoanProductRepository.findAll(pageRequest);
        processProducts(productPage.getContent(), "신용대출 (테스트)", this::processIndividualCreditProduct);
    }

    @Override
    @Transactional
    public void createPostsFromLoanProducts() {
        log.info("일반대출 상품 데이터 동기화 시작 (테스트: 5개만 처리)...");
        Pageable pageRequest = PageRequest.of(0, 5);
        Page<LoanProduct> productPage = loanProductRepository.findAll(pageRequest);
        processProducts(productPage.getContent(), "일반대출 (테스트)", this::processIndividualLoanProduct);
    }

    @Override
    @Transactional
    public void createPostsFromSupportProducts() {
        log.info("공공지원금 상품 데이터 동기화 시작 (테스트: 5개만 처리)...");
        Pageable pageRequest = PageRequest.of(0, 5);
        Page<SupportProduct> productPage = supportProductRepository.findAll(pageRequest);
        processProducts(productPage.getContent(), "공공지원금 (테스트)", this::processIndividualSupportProduct);
    }

    // ------------------------------------------------------------------
    // 아래 헬퍼 메서드들은 실제 운영 환경을 위한 코드이므로 그대로 유지합니다.
    // ------------------------------------------------------------------

    private <T> void processProductsInPages(PageableFunction<T> pagedFinder, String productType, ProductProcessor<T> processor) {
        int page = 0;
        final int PAGE_SIZE = 100;
        Page<T> productPage;

        do {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);
            productPage = pagedFinder.apply(pageable);

            log.info("[{}] 처리 진행: {}/{} 페이지 (현재 페이지 항목: {}개)",
                    productType, page + 1, productPage.getTotalPages(), productPage.getNumberOfElements());

            processProducts(productPage.getContent(), productType, processor);

            page++;
        } while (productPage.hasNext());

        log.info("{} 상품 Post 생성 전체 완료 - 총 {}개 페이지 처리 완료", productType, productPage.getTotalPages());
    }

    private <T> void processProducts(List<T> products, String productType, ProductProcessor<T> processor) {
        int successCount = 0;
        for (T product : products) {
            try {
                processor.process(product);
                successCount++;
            } catch (Exception e) {
                log.error("[{}] 개별 상품 처리 실패: {} - 원인: {}", productType, product.toString(), e.getMessage());
            }
        }
        log.info("[{}] 현재 배치 처리 완료 (성공: {}/{})", productType, successCount, products.size());
    }

    @FunctionalInterface
    private interface PageableFunction<T> {
        Page<T> apply(Pageable pageable);
    }

    @FunctionalInterface
    private interface ProductProcessor<T> {
        void process(T product) throws Exception;
    }

    // ------------------------------------------------------------------
    // 이하 개별 처리 및 변환 메서드들은 모두 그대로 유지합니다.
    // ------------------------------------------------------------------

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualCreditProduct(CreditLoanProduct creditProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("CreditLoanProduct", creditProduct.getId())) {
            Post post = createPostFromCreditLoanProduct(creditProduct);
            Post savedPost = postRepository.save(post);
            updatePostWithAISummary(savedPost, () -> summaryService.generateForCredit(creditProduct.getId()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualLoanProduct(LoanProduct loanProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("LoanProduct", loanProduct.getId())) {
            Post post = createPostFromLoanProduct(loanProduct, new String[]{"작성 예정", "작성 예정"});
            Post savedPost = postRepository.save(post);
            updatePostWithAISummary(savedPost, () -> summaryService.generateForLoan(loanProduct.getId()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualSupportProduct(SupportProduct supportProduct) {
        if (!postRepository.existsBySourceTableAndSourceId("SupportProduct", supportProduct.getId())) {
            Post post = createPostFromSupportProduct(supportProduct);
            Post savedPost = postRepository.save(post);
            updatePostWithAISummary(savedPost, () -> summaryService.generateForSupport(supportProduct.getId()));
        }
    }

    private void updatePostWithAISummary(Post post, SummaryGenerator generator) {
        try {
            String[] summaryResult = generator.generate();
            if (summaryResult != null && summaryResult.length >= 2) {
                post.setDetailContent(summaryResult[0]);
                post.setSummary(summaryResult[1]);
                postRepository.save(post);
                log.debug("Post({}) AI 요약 생성 및 업데이트 완료", post.getPostId());
            }
        } catch (Exception e) {
            log.warn("Post({}) AI 요약 생성 실패, 기본 Post는 유지: {}", post.getPostId(), e.getMessage());
        }
    }

    @FunctionalInterface
    private interface SummaryGenerator {
        String[] generate() throws Exception;
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

    private Post createPostFromCreditLoanProduct(CreditLoanProduct creditProduct) {
        return Post.builder()
                .postType(PostType.LOAN)
                .productName(creditProduct.getFinPrdtNm())
                .summary("작성 예정")
                .detailContent("작성 예정")
                .deadline(creditProduct.getDclsEndDay())
                .companyName(creditProduct.getKorCoNm())
                .joinLink(creditProduct.getRltSite())
                .sourceTable("CreditLoanProduct")
                .sourceId(creditProduct.getId())
                .build();
    }

    private Post createPostFromLoanProduct(LoanProduct loanProduct, String[] str) {
        return Post.builder()
                .postType(PostType.LOAN)
                .productName(loanProduct.getProductName())
                .summary(str[1])
                .detailContent(str[0])
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
                .summary(supportProduct.getServicePurposeSummary() != null ?
                        supportProduct.getServicePurposeSummary() : "작성 예정")
                .detailContent("작성 예정")
                .deadline(supportProduct.getEndDay())
                .companyName(supportProduct.getDepartmentName())
                .joinLink(supportProduct.getOnlineApplicationUrl() != null ?
                        supportProduct.getOnlineApplicationUrl() :
                        supportProduct.getDetailUrl())
                .sourceTable("SupportProduct")
                .sourceId(supportProduct.getId())
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