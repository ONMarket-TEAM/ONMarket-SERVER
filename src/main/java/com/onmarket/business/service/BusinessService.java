package com.onmarket.business.service;

import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;

import java.util.List;

public interface BusinessService {

    /** 사업자 등록 */
    BusinessResponse registerBusiness(String email, BusinessRequest request);

    /** 회원 이메일로 사업자 목록 조회 */
    List<BusinessResponse> getMemberBusinesses(String email);

    /** 회원 ID로 사업자 목록 조회 */
    List<BusinessResponse> getMemberBusinesses(Long memberId);
}
