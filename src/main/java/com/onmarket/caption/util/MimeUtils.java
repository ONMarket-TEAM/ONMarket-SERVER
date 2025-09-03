// com/onmarket/caption/util/MimeUtils.java
package com.onmarket.caption.util;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public class MimeUtils {
    public static String guessContentType(MultipartFile file) {
        String type = file.getContentType();
        return type != null ? type : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}