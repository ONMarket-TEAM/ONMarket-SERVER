package com.onmarket.caption.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PresignRequest {
    private String filename;
    private String contentType; // image/jpeg, image/png
}