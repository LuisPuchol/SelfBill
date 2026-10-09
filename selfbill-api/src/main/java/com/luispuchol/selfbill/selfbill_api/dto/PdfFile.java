package com.luispuchol.selfbill.selfbill_api.dto;

public record PdfFile(String filename, byte[] content) {
}
