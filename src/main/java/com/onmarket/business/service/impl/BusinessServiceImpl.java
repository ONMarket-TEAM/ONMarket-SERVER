package com.onmarket.business.service.impl;

import com.onmarket.business.domain.Business;
import com.onmarket.business.domain.enums.BusinessStatus;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final MemberService memberService;

    @Override
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
    public List<BusinessResponse> getMemberBusinesses(String email) {
        Member member = memberService.findByEmail(email);
        return getBusinessesByMember(member);
    }

    @Override
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
}
