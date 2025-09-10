package com.onmarket.business.service;

import com.onmarket.business.dto.BusinessUpdateRequest;
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

    /** 사업장 단건조회(이메일 기반) */
    BusinessResponse getMyBusiness(String email, Long businessId);

    /** 사업 정보 수정(이메일 기반) */
    BusinessResponse updateMyBusiness(String email, Long businessId, BusinessUpdateRequest request);

    /** 사업장 삭제 */
    void deleteMyBusiness(String email, Long businessId);

}
