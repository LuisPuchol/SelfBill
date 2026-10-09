package com.luispuchol.selfbill.selfbill_api.service;

public record SmtpAccount(String email, String host, Integer port, String password) {
}
