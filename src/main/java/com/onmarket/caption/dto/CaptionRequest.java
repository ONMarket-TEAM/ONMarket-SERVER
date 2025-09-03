package com.onmarket.caption.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CaptionRequest {
    // 방식 A: 파일 업로드 (최대 10장)
    @Size(max = 10, message = "이미지는 최대 10장까지 가능합니다.")
    private List<MultipartFile> files;

    // 방식 B: URL 전달 (최대 10개)
    @Size(max = 10, message = "이미지 URL은 최대 10개까지 가능합니다.")
    private List<String> s3Urls;

    private OptionStyle options = new OptionStyle();

    // 반드시 포함할 문구 (철자/띄어쓰기 동일 포함)
    private String mustInclude;

    // 추가 맥락(선택)
    private String contextHint;
}