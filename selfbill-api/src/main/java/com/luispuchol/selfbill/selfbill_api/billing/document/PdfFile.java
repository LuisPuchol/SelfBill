package com.luispuchol.selfbill.selfbill_api.billing.document;

public record PdfFile(String filename, byte[] content) {
}
