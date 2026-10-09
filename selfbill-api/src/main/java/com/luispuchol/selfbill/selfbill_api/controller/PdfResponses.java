package com.luispuchol.selfbill.selfbill_api.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.luispuchol.selfbill.selfbill_api.dto.PdfFile;

final class PdfResponses {

    private PdfResponses() {
    }

    static ResponseEntity<byte[]> attachment(PdfFile pdf) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(pdf.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
