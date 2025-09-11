package com.onmarket.recommendation;

import com.onmarket.business.domain.Business;
import com.onmarket.business.domain.enums.BusinessStatus;
import com.onmarket.business.repository.BusinessRepository;
import com.onmarket.member.domain.Member;
import com.onmarket.member.service.MemberService;
import com.onmarket.post.domain.Post;
import com.onmarket.post.domain.PostType;
import com.onmarket.post.repository.PostRepository;
import com.onmarket.recommendation.dto.RecommendationResponse;
import com.onmarket.recommendation.repository.InterestScoreRepository;
import com.onmarket.recommendation.repository.UserInteractionRepository;
import com.onmarket.recommendation.service.AgeFilterService;
import com.onmarket.recommendation.service.PriorityRecommendationService;
import com.onmarket.recommendation.service.RecommendationService;
import com.onmarket.recommendation.service.RegionFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {

    @InjectMocks
    private RecommendationService recommendationService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private InterestScoreRepository interestScoreRepository;

    @Mock
    private UserInteractionRepository userInteractionRepository;

    @Mock
    private PriorityRecommendationService priorityRecommendationService;

    @Mock
    private RegionFilterService regionFilterService;

    @Mock
    private AgeFilterService ageFilterService;

    @Mock
    private BusinessRepository businessRepository;

    private Member testMember;
    private Business testBusiness;
    private List<Post> posts;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testMember = Member.builder()
                .email("test@test.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .mainBusinessId(1L)
                .build();

        testBusiness = Business.builder()
                .businessId(1L)
                .status(BusinessStatus.ACTIVE)
                .sidoName("서울")
                .sigunguName("강남구")
                .businessName("TestBusiness")
                .build();

        posts = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            Post post = Post.builder()
                    .postId(i)
                    .postType(i % 2 == 0 ? PostType.LOAN : PostType.SUPPORT)
                    .productName("Product" + i)
                    .companyName("Company" + i)
                    .build();

            // ✅ createdAt 강제로 주입 (BaseTimeEntity 때문에 builder로는 불가)
            ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now().minusDays(i));

            posts.add(post);
        }

        when(cacheManager.getCache("userRecommendations")).thenReturn(cache);
    }

    @Test
    void testGetPersonalizedRecommendations_withCache() {
        // 캐시 존재 시
        List<RecommendationResponse> cachedList = Collections.singletonList(
                RecommendationResponse.builder().postId(1L).build());
        when(cache.get("test@test.com", List.class)).thenReturn(cachedList);

        when(memberService.findByEmail("test@test.com")).thenReturn(testMember);
        when(businessRepository.findById(1L)).thenReturn(Optional.of(testBusiness));

        List<RecommendationResponse> result = recommendationService.getPersonalizedRecommendations("test@test.com");

        assertThat(result).hasSize(1);
        verify(cache, times(1)).get("test@test.com", List.class);
        verifyNoMoreInteractions(postRepository);
    }
}
