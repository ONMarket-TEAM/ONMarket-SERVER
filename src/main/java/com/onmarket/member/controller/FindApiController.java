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
    public ApiResponse<String> findId(@RequestBody FindIdRequest request) {
        String memberId = memberService.findId(request.getUsername(), request.getPhone());
        return ApiResponse.success(ResponseCode.ID_FIND_SUCCESS, memberId);
    }

}
