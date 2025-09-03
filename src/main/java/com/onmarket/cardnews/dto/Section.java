package com.onmarket.cardnews.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {
    private String heading;
    private String text; // optional
    private List<String> bullets; // optional
}