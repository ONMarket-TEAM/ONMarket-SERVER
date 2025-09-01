package com.onmarket.member.controller;

import com.onmarket.common.response.ApiResponse;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.dto.FindIdRequest;
import com.onmarket.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member Public API", description = "비로그인 회원 관련 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class FindApiController {

    private final MemberService memberService;

    @PostMapping("/find-id")
    @Operation(summary = "아이디 찾기", description = "회원가입 시 입력한 이름/휴대폰 번호를 기반으로 이메일을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "아이디 찾기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수값 누락, 잘못된 전화번호 형식 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ApiResponse<String> findId(@RequestBody FindIdRequest request) {
        String memberId = memberService.findId(request.getUsername(), request.getPhone());
        return ApiResponse.success(ResponseCode.ID_FIND_SUCCESS, memberId);
    }
}
