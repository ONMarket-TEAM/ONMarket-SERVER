package com.onmarket.member.controller;

import com.onmarket.member.dto.SignupRequest;
import com.onmarket.member.service.impl.SignupServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SignupApiController {

    private final SignupServiceImpl signupService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        signupService.signup(request);
        return ResponseEntity.ok("회원가입 완료");
    }
}
