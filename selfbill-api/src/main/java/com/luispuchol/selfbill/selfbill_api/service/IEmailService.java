package com.luispuchol.selfbill.selfbill_api.service;

public interface IEmailService {

    void sendInvoiceEmail(Integer invoiceId);
}
