package com.onmarket.member.service.impl;

import com.onmarket.business.exception.BusinessException;
import com.onmarket.common.response.ResponseCode;
import com.onmarket.member.domain.Member;
import com.onmarket.member.repository.MemberRepository;
import com.onmarket.member.service.ProfileImageService;
import com.onmarket.s3.dto.PresignPutResponse;
import com.onmarket.s3.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileImageServiceImpl implements ProfileImageService {
    private final MemberRepository memberRepository;
    private final S3PresignService s3;

    @Override
    @Transactional(readOnly = true)
    public PresignPutResponse presignForUpload(Long memberId, String filename, String contentType) {

        // 입력값 검증
        if (!StringUtils.hasText(filename) || !StringUtils.hasText(contentType)) {
            throw new BusinessException(ResponseCode.INVALID_REQUEST_PARAM);
        }

        // 회원 존재 여부 확인 (선택사항이지만 보안상 좋음)
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ResponseCode.MEMBER_NOT_FOUND);
        }

        // 예: profiles/{memberId}/<uuid>-<filename>
        String dir = "profiles/" + memberId;
        String key = s3.buildKey(dir, filename);

        URL url = s3.generatePutUrl(key, contentType);
        return new PresignPutResponse(url.toString(), key, contentType);
    }

    @Override
    public ImageUrlResponse confirmUpload(Long memberId, String key) {

        // 입력값 검증
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ResponseCode.INVALID_IMAGE_KEY);
        }

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));

        m.changeProfileImage(key); // 영속 상태면 flush 시 자동 반영
        String viewUrl = s3.generateGetUrl(key).toString(); // 바로 보여줄 URL
        return new ImageUrlResponse(key, viewUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageUrlResponse current(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        String key = m.getProfileImage();
        String url = (key == null) ? null : s3.generateGetUrl(key).toString();
        return new ImageUrlResponse(key, url);
    }

    @Override
    public void deleteProfileImage(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        String oldImage = m.getProfileImage();
        m.changeProfileImage(null); // DB에서 null로 설정

        if (StringUtils.hasText(oldImage)) {
            // 로그 출력 (필요시 S3에서 실제 파일 삭제 로직 추가)
            System.out.println("프로필 이미지 삭제됨: " + oldImage);
        }
    }
}
