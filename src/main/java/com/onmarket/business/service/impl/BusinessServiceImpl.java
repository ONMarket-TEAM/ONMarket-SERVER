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
import com.onmarket.recommendation.domain.MainBusinessChangedEvent;
import com.onmarket.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final MemberService memberService;
    private final RecommendationService recommendationService; // 추가
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BusinessResponse registerBusiness(String email, BusinessRequest request) {
        Member member = memberService.findByEmail(email);

        // 이미 같은 사업장이 등록된 경우 체크
        if (businessRepository.existsByMemberAndBusinessName(member, request.getBusinessName())) {
            throw new BusinessException(ResponseCode.BUSINESS_ALREADY_EXISTS);
        }

        Business business = Business.builder()
                .member(member)
                .businessName(request.getBusinessName())
                .industry(request.getIndustry())
                .businessType(request.getBusinessType())
                .sidoName(request.getSidoName())
                .sigunguName(request.getSigunguName())
                .establishedYear(request.getEstablishedYear())
                .annualRevenue(request.getAnnualRevenue())
                .employeeCount(request.getEmployeeCount())
                .status(BusinessStatus.ACTIVE)
                .build();

        Business saved = businessRepository.save(business);

        if (member.getMainBusinessId() == null) {
            member.updateMainBusiness(saved.getBusinessId());
        }

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

        return businesses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Entity → DTO 변환 */
    private BusinessResponse toResponse(Business b) {
        return new BusinessResponse(
                b.getBusinessId(),
                b.getBusinessName(),
                b.getIndustry(),
                b.getBusinessType(),
                b.getSidoName(),
                b.getSigunguName(),
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

        // 변경 전 상태 저장 (대표 사업장인 경우만)
        boolean isMainBusiness = member.getMainBusinessId() != null &&
                member.getMainBusinessId().equals(businessId);
        boolean needsRecommendationRebuild = false;

        // 추천에 영향을 주는 필드 변경 감지
        if (isMainBusiness) {
            needsRecommendationRebuild = checkRecommendationRelevantChanges(business, req);
        }

        // 사업장명 변경 시 중복 체크
        if (req.getBusinessName() != null &&
                !req.getBusinessName().equals(business.getBusinessName()) &&
                businessRepository.existsByMemberAndBusinessName(member, req.getBusinessName())) {
            throw new BusinessException(ResponseCode.BUSINESS_DUPLICATED);
        }

        // 부분 업데이트 적용
        if (req.getBusinessName() != null) business.changeBusinessName(req.getBusinessName());
        if (req.getIndustry()      != null) business.changeIndustry(req.getIndustry());
        if (req.getBusinessType()  != null) business.changeBusinessType(req.getBusinessType());
        if (req.getSidoName()      != null || req.getSigunguName() != null) {business.changeRegion(req.getSidoName(), req.getSigunguName());
        }
        if (req.getAnnualRevenue() != null) business.changeAnnualRevenue(req.getAnnualRevenue());
        if (req.getEmployeeCount() != null) business.changeEmployeeCount(req.getEmployeeCount());
        if (req.getEstablishedYear()!= null) business.changeEstablishedYear(req.getEstablishedYear());

        if (isMainBusiness && needsRecommendationRebuild) {
            eventPublisher.publishEvent(new MainBusinessChangedEvent(
                    email, businessId, businessId,
                    MainBusinessChangedEvent.BusinessChangeType.BUSINESS_INFO_UPDATED));
        }

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

    @Override
    @Transactional
    public void deleteMyBusiness(String email, Long businessId) {
        Member member = findMember(email);
        Business business = findOwnedBusiness(member, businessId);

        boolean wasMainBusiness = member.getMainBusinessId() != null &&
                member.getMainBusinessId().equals(businessId);

        businessRepository.delete(business);

        if (wasMainBusiness) {
            List<Business> remaining = businessRepository.findByMember(member);
            if (!remaining.isEmpty()) {
                Business newMainBusiness = remaining.get(0);
                member.updateMainBusiness(newMainBusiness.getBusinessId());

                // 🔥 새로운 대표 사업장 기준으로 추천 재구축
                log.info("대표 사업장 삭제 후 새 대표 사업장 기준 추천 재구축: Member {}, 신규 대표: {}",
                        member.getEmail(), newMainBusiness.getBusinessId());
                recommendationService.rebuildRecommendationsForBusiness(member, newMainBusiness);
            } else {
                member.updateMainBusiness(null);
                // 🔥 사업장이 없는 경우 추천 캐시 삭제
                recommendationService.clearRecommendationsForMember(member);
            }
        }
    }


    @Override
    @Transactional
    public void changeMainBusiness(String email, Long businessId) {
        Member member = findMember(email);
        Long previousBusinessId = member.getMainBusinessId();

        // 이전 대표 사업장과 다른 경우에만 추천 재구축
        if (!businessId.equals(member.getMainBusinessId())) {
            member.updateMainBusiness(businessId);

            eventPublisher.publishEvent(new MainBusinessChangedEvent(
                    email, previousBusinessId, businessId,
                    MainBusinessChangedEvent.BusinessChangeType.BUSINESS_SWITCH));
        }
    }
    /**
     * 추천에 영향을 주는 필드 변경 여부 확인
     */
    private boolean checkRecommendationRelevantChanges(Business business, BusinessUpdateRequest req) {
        // 지역 정보 변경 확인
        boolean regionChanged = (req.getSidoName() != null && !req.getSidoName().equals(business.getSidoName())) ||
                (req.getSigunguName() != null && !req.getSigunguName().equals(business.getSigunguName()));

        // 업종 변경 확인
        boolean industryChanged = req.getIndustry() != null && !req.getIndustry().equals(business.getIndustry());

        // 사업 형태 변경 확인 (추천에 영향을 줄 수 있는 경우)
        boolean businessTypeChanged = req.getBusinessType() != null &&
                !req.getBusinessType().equals(business.getBusinessType());

        return regionChanged || industryChanged || businessTypeChanged;
    }
}
