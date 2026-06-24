package com.luispuchol.selfbill.selfbill_api.service;

public interface IPdfService {

    byte[] generateInvoicePdf(Integer invoiceId);
}
