package com.onmarket.member.controller;

import com.onmarket.member.dto.LoginRequest;
import com.onmarket.member.dto.LoginResponse;
import com.onmarket.member.service.impl.LoginServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginApiController {

    private final LoginServiceImpl loginService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.ok(response);
    }
}
