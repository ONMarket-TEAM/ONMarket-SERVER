package com.onmarket.member.service;

import com.onmarket.member.dto.LoginRequest;
import com.onmarket.member.dto.LoginResponse;

public interface LoginService {
    LoginResponse login(LoginRequest request);

    void logout(String token);

}
