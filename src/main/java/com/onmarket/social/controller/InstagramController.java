package com.onmarket.social.controller;

import com.onmarket.member.domain.Member;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.oauth.jwt.JwtTokenProvider;
import com.onmarket.member.exception.LoginException;
import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.social.dto.InstagramLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Instagram API", description = "Instagram 모의 연동 API")
@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class InstagramController {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /** JWT 토큰에서 이메일 추출 */
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

    /** Instagram 로그인 (아이디만 저장) */
    @PostMapping("/login")
    @Operation(summary = "Instagram 로그인", description = "아이디 입력만으로 Instagram 로그인 처리")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<String> login(
            HttpServletRequest request,
            @RequestBody InstagramLoginRequest loginRequest
    ) {
        String email = extractEmailFromToken(request);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new LoginException(ResponseCode.MEMBER_NOT_FOUND));

        member.changeInstagramUsername(loginRequest.getUsername());
        memberRepository.save(member);

        return ApiResponse.success(
                ResponseCode.INSTAGRAM_LOGIN_SUCCESS,
                member.getDisplayInstagramUsername()
        );
    }


    /** Instagram 로그아웃 */
    @PostMapping("/logout")
    @Operation(summary = "Instagram 로그아웃", description = "Instagram 연결을 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<String> logout(HttpServletRequest request) {
        String email = extractEmailFromToken(request);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new LoginException(ResponseCode.MEMBER_NOT_FOUND));

        member.changeInstagramUsername(null);
        memberRepository.save(member);

        return ApiResponse.success(ResponseCode.INSTAGRAM_LOGOUT_SUCCESS,
                "Instagram 로그아웃 완료");
    }

    // Instagram 연결 상태 조회
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getInstagramStatus(HttpServletRequest request) {
        String email = extractEmailFromToken(request);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new LoginException(ResponseCode.MEMBER_NOT_FOUND));

        Map<String, Object> result = new HashMap<>();
        result.put("connected", member.hasInstagramConnected());
        result.put("username", member.getDisplayInstagramUsername());

        return ApiResponse.success(ResponseCode.INSTAGRAM_STATUS_SUCCESS, result);
    }

}
