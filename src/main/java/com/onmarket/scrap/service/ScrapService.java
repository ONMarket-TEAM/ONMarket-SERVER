package com.onmarket.scrap.service;

import com.onmarket.post.dto.PostListResponse;
import com.onmarket.scrap.dto.ScrapToggleResponse;

import java.util.List;

public interface ScrapService {
    ScrapToggleResponse toggleScrap(String email, Long postId);
    List<PostListResponse> getMyScraps(String email);
    boolean isScrapedByMe(String email, Long postId);
    Long getScrapCount(Long postId);
}
