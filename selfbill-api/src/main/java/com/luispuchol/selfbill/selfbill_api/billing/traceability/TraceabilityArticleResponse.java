package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "An article with at least one delivery note line in the period")
@Builder
public record TraceabilityArticleResponse(
        Integer articleId,
        Integer articleCode,
        String articleName) {
}
