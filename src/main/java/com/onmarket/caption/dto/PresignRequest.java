package com.onmarket.caption.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PresignRequest {
    @NotBlank private String filename;
    @NotBlank private String contentType; // image/jpeg, image/png
}