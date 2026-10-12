package com.luispuchol.selfbill.selfbill_api.catalog.client;

import lombok.Builder;

@Builder
public record ClientFilter(
        String name,
        String businessName,
        Integer codeFrom,
        Integer codeTo) {
}
