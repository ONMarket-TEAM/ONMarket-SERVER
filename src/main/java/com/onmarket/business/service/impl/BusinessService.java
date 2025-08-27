package com.onmarket.business.service.impl;

import com.onmarket.business.domain.Business;
import com.onmarket.business.domain.enums.BusinessStatus;
import com.onmarket.business.dto.BusinessRequest;
import com.onmarket.business.dto.BusinessResponse;
import com.onmarket.business.repository.BusinessRepository;
import com.onmarket.member.domain.Member;
import com.onmarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final MemberRepository memberRepository;

    public BusinessResponse registerBusiness(String email, BusinessRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

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

        return new BusinessResponse(
                saved.getBusinessId(),
                saved.getIndustry(),
                saved.getBusinessType(),
                saved.getRegionCodeId(),
                saved.getEstablishedYear(),
                saved.getAnnualRevenue(),
                saved.getEmployeeCount(),
                saved.getStatus()
        );
    }

    public List<BusinessResponse> getMemberBusinesses(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        return businessRepository.findByMember(member).stream()
                .map(b -> new BusinessResponse(
                        b.getBusinessId(),
                        b.getIndustry(),
                        b.getBusinessType(),
                        b.getRegionCodeId(),
                        b.getEstablishedYear(),
                        b.getAnnualRevenue(),
                        b.getEmployeeCount(),
                        b.getStatus()
                ))
                .collect(Collectors.toList());
    }

    public List<BusinessResponse> getMemberBusinesses(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        return businessRepository.findByMember(member).stream()
                .map(b -> new BusinessResponse(
                        b.getBusinessId(),
                        b.getIndustry(),
                        b.getBusinessType(),
                        b.getRegionCodeId(),
                        b.getEstablishedYear(),
                        b.getAnnualRevenue(),
                        b.getEmployeeCount(),
                        b.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
