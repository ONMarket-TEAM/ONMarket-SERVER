package com.onmarket.caption.controller;

import com.onmarket.caption.dto.CaptionRequest;
import com.onmarket.caption.dto.CaptionResponse;
import com.onmarket.caption.dto.OptionStyle;
import com.onmarket.caption.service.CaptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/captions")
@RequiredArgsConstructor
public class CaptionController {

    private final CaptionService captionService;

    // A) 파일 여러 장 업로드 (최대 10장)
    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CaptionResponse uploadAndCaption(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart(value = "options.tone", required = false) String tone,
            @RequestPart(value = "options.language", required = false) String language,
            @RequestPart(value = "options.withHashtags", required = false) Boolean withHashtags,
            @RequestPart(value = "options.withEmojis", required = false) Boolean withEmojis,
            @RequestPart(value = "options.variations", required = false) Integer variations,
            @RequestPart(value = "options.maxChars", required = false) Integer maxChars,
            @RequestPart(value = "mustInclude", required = false) String mustInclude,
            @RequestPart(value = "contextHint", required = false) String contextHint
    ) {
        CaptionRequest req = new CaptionRequest();
        req.setFiles(files);

        if (req.getOptions() == null) req.setOptions(new OptionStyle());
        var opt = req.getOptions();

        if (tone != null) opt.setTone(tone);
        if (language != null) opt.setLanguage(language);
        if (withHashtags != null) opt.setWithHashtags(withHashtags);
        // 이모지는 금지(프론트에서 false로 보내더라도 여기서 한 번 더 강제)
        opt.setWithEmojis(false);
        if (variations != null) opt.setVariations(variations);
        if (maxChars != null) opt.setMaxChars(maxChars);

        req.setMustInclude(mustInclude);
        req.setContextHint(contextHint);

        return captionService.createCaptions(req);
    }

    // B) URL 여러 개 전달
    @PostMapping(
            path = "/from-urls",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CaptionResponse captionFromUrls(@Valid @RequestBody CaptionRequest req) {
        // req.s3Urls에 최대 10개 URL
        return captionService.createCaptions(req);
    }
}