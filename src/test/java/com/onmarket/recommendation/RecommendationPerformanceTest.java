package com.onmarket;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.onmarket.recommendation.dto.RecommendationResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

@Test
public class RecommendationPerformanceTest {

    @Test
    public void testRecommendationResponseTime() {
        // 1000명의 사용자에 대해 동시 추천 요청
        List<String> testEmails = generateTestEmails(1000);

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<List<RecommendationResponse>>> futures = testEmails.stream()
                .map(email -> CompletableFuture.supplyAsync(() ->
                        recommendationService.getPersonalizedRecommendations(email)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long totalTime = System.currentTimeMillis() - startTime;
        double avgResponseTime = totalTime / (double) testEmails.size();

        // 평균 응답시간 1초 이하 목표
        assertThat(avgResponseTime).isLessThan(1000.0);
    }
}