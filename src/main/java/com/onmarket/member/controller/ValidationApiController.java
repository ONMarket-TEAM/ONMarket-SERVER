package com.onmarket.member.controller;

import com.onmarket.member.service.impl.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
public class ValidationApiController {

    private final ValidationService validationService;

    // 이메일 중복 체크
    @GetMapping("/check/email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean isDuplicate = validationService.isEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    // 닉네임 중복 체크
    @GetMapping("/check/nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = validationService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }
}
