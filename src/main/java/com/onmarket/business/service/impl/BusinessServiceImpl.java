package com.onmarket.business.service.impl;

import com.onmarket.business.domain.Business;
import com.onmarket.business.dto.BusinessUpdateRequest;
import com.onmarket.business.domain.enums.AnnualRevenue;
import com.onmarket.business.domain.enums.BusinessStatus;
import com.onmarket.business.domain.enums.BusinessType;
import com.onmarket.business.domain.enums.Industry;
import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.exception.BusinessException;
import com.onmarket.business.repository.BusinessRepository;
import com.onmarket.member.domain.Member;
import com.onmarket.member.service.MemberService;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.business.service.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final MemberService memberService;

    @Override
    @Transactional
    public BusinessResponse registerBusiness(String email, BusinessRequest request) {
        Member member = memberService.findByEmail(email);

        // 이미 같은 사업장이 등록된 경우 체크
        if (businessRepository.existsByMemberAndIndustryAndBusinessTypeAndRegionCodeId(
                member,
                request.getIndustry(),
                request.getBusinessType(),
                request.getRegionCodeId())) {
            throw new BusinessException(ResponseCode.BUSINESS_ALREADY_EXISTS);
        }

        Business business = Business.builder()
                .member(member)
                .industry(request.getIndustry())
                .businessType(request.getBusinessType())
                .regionCodeId(request.getRegionCodeId())
                .establishedYear(request.getEstablishedYear())
                .annualRevenue(request.getAnnualRevenue())
                .employeeCount(request.getEmployeeCount())
                .status(BusinessStatus.ACTIVE)
                .build();

        Business saved = businessRepository.save(business);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessResponse> getMemberBusinesses(String email) {
        Member member = memberService.findByEmail(email);
        return getBusinessesByMember(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessResponse> getMemberBusinesses(Long memberId) {
        Member member = memberService.findById(memberId);
        return getBusinessesByMember(member);
    }

    /** 공통 비즈니스 조회 로직 */
    private List<BusinessResponse> getBusinessesByMember(Member member) {
        List<Business> businesses = businessRepository.findByMember(member);
        if (businesses.isEmpty()) {
            throw new BusinessException(ResponseCode.BUSINESS_NOT_FOUND);
        }

        return businesses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Entity → DTO 변환 */
    private BusinessResponse toResponse(Business b) {
        return new BusinessResponse(
                b.getBusinessId(),
                b.getIndustry(),
                b.getBusinessType(),
                b.getRegionCodeId(),
                b.getEstablishedYear(),
                b.getAnnualRevenue(),
                b.getEmployeeCount(),
                b.getStatus()
        );
    }

    /** 사업장 단건 조회(이메일 기반) */
    @Override
    @Transactional(readOnly = true)
    public BusinessResponse getMyBusiness(String email, Long businessId) {
        Member member = findMember(email);
        Business business = findOwnedBusiness(member, businessId);
        return BusinessResponse.from(business);
    }

    /** 선택한 사업장 부분 수정 */
    @Override
    @Transactional
    public BusinessResponse updateMyBusiness(String email, Long businessId, BusinessUpdateRequest req) {
        Member member = findMember(email);
        Business business = findOwnedBusiness(member, businessId);

        // (1) 변경 예정 값 계산
        Industry newIndustry   = req.getIndustry()      != null ? req.getIndustry()      : business.getIndustry();
        BusinessType newType       = req.getBusinessType()  != null ? req.getBusinessType()  : business.getBusinessType();
        String        newRegion     = req.getRegionCodeId()  != null ? req.getRegionCodeId()  : business.getRegionCodeId();
        AnnualRevenue newRevenue    = req.getAnnualRevenue() != null ? req.getAnnualRevenue() : business.getAnnualRevenue();

        // (2) 업종/형태/지역 조합이 변경되는 경우, 같은 회원의 중복 사업장 방지
        boolean keyChanged =
                newIndustry != business.getIndustry()
                        || newType     != business.getBusinessType()
                        || !newRegion.equals(business.getRegionCodeId());

        if (keyChanged &&
                businessRepository.existsByMemberAndIndustryAndBusinessTypeAndRegionCodeId(member, newIndustry, newType, newRegion)) {
            throw new BusinessException(ResponseCode.BUSINESS_DUPLICATED);
        }

        // (3) 부분 업데이트 적용
        if (req.getIndustry()      != null) business.changeIndustry(req.getIndustry());
        if (req.getBusinessType()  != null) business.changeBusinessType(req.getBusinessType());
        if (req.getRegionCodeId()  != null) business.changeRegion(req.getRegionCodeId());
        if (req.getAnnualRevenue() != null) business.changeAnnualRevenue(req.getAnnualRevenue());
        if (req.getEmployeeCount() != null) business.changeEmployeeCount(req.getEmployeeCount());
        if (req.getEstablishedYear()!= null) business.changeEstablishedYear(req.getEstablishedYear());

        // JPA Dirty Checking으로 자동 반영
        return BusinessResponse.from(business);
    }

    // ===== 내부 유틸 =====
    private Member findMember(String email) {
        return memberService.findByEmail(email);
    }

    private Business findOwnedBusiness(Member owner, Long businessId) {
        Business b = businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessException(ResponseCode.BUSINESS_NOT_FOUND));
        if (!b.getMember().getMemberId().equals(owner.getMemberId())) {
            throw new BusinessException(ResponseCode.BUSINESS_FORBIDDEN);
        }
        return b;
    }
}
