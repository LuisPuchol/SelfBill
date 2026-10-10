package com.luispuchol.selfbill.selfbill_api.settings.profile;

public record SmtpAccount(String email, String host, Integer port, String password) {
}
