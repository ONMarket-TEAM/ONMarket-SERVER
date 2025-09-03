package com.onmarket.cardnews.dto;

import java.util.List;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CardNewsRequest {
    /** Either pass rowText directly, or structured DTOs below. */
    private String rowText;

    // Structured inputs (optional)
    private LoanProductDto loanProduct;
    private CreditLoanProductDto creditLoanProduct;
    private List<CreditLoanOptionDto> creditLoanOptions;
    private SupportServiceDto supportService;
    private List<SupportConditionDto> supportConditions;

    /** Optional override for background prompt */
    private String bgPrompt;

    /** Output image options */
    @Min(256) private Integer width; // default: 1024
    @Min(256) private Integer height; // default: 1536
    @Min(1) private Integer deviceScaleFactor; // default: 2

    /** When true, API returns dataURL instead of octet-stream */
    private Boolean returnDataUrl;
}