package com.onmarket.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3PresignService svc;

    @PostMapping(value = "/presign-put", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> presignPut(@RequestParam String dir,
                                          @RequestParam String filename,
                                          @RequestParam String contentType) {
        if (!StringUtils.hasText(dir) || !StringUtils.hasText(filename) || !StringUtils.hasText(contentType)) {
            return Map.of("ok", false, "error", "Missing params");
        }
        String key = svc.buildKey(dir, filename);
        URL url = svc.generatePutUrl(key, contentType);
        return Map.of("ok", true, "uploadUrl", url.toString(), "key", key);
    }

    @GetMapping("/presign-get")
    public Map<String, Object> presignGet(@RequestParam String key) {
        URL url = svc.generateGetUrl(key);
        return Map.of("ok", true, "downloadUrl", url.toString());
    }
}