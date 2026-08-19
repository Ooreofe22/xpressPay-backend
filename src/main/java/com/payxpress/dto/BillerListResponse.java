package com.payxpress.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillerListResponse(String responseCode, String responseMessage, Data data) {

    public record Data(boolean hasNextRecord, int totalCount, List<ProductDTO> productDTOList) {}

    public record ProductDTO(
            String name,
            String uniqueCode,
            boolean lookUp,
            boolean fixedAmount,
            BigDecimal amount,
            BigDecimal minimumAmount,
            BigDecimal maximumAmount,
            String imageUrl,
            String billerName,
            CategoryDTO categoryDTO
    ) {}

    public record CategoryDTO(int id, String name) {}
}
