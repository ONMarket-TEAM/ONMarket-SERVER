package com.onmarket.scrap.controller;

import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.exception.LoginException;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.post.dto.PostListResponse;
import com.onmarket.scrap.dto.ScrapToggleResponse;
import com.onmarket.scrap.service.ScrapService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scraps")
@RequiredArgsConstructor
@Tag(name = "스크랩 API")
@SecurityRequirement(name = "BearerAuth")
public class ScrapApiController {

    private final ScrapService scrapService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * ❤️ 스크랩 토글 (하트 버튼 클릭)
     */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleScrap(
            HttpServletRequest request,
            @RequestParam Long postId) {
        String email = extractEmailFromToken(request);
        ScrapToggleResponse response = scrapService.toggleScrap(email, postId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "isScraped", response.isScraped(),
                "scrapCount", response.getScrapCount(),
                "message", response.isScraped() ? "스크랩했습니다 ❤️" : "스크랩 해제했습니다 🤍"
        ));
    }

    /**
     * 📋 내 스크랩 목록
     */
    @GetMapping("/my")
    public ResponseEntity<List<PostListResponse>> getMyScraps(HttpServletRequest request) {
        String email = extractEmailFromToken(request);

        List<PostListResponse> myScraps = scrapService.getMyScraps(email);
        return ResponseEntity.ok(myScraps);
    }

    private String extractEmailFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_NOT_FOUND);
        }

        String token = header.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new LoginException(ResponseCode.AUTH_TOKEN_INVALID);
        }

        return jwtTokenProvider.getEmail(token);
    }
}
