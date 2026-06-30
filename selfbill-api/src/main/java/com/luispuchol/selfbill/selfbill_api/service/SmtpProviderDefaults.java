package com.luispuchol.selfbill.selfbill_api.service;

import java.util.Locale;
import java.util.Map;

final class SmtpProviderDefaults {

    private record SmtpEndpoint(String host, int port) {
    }

    private static final Map<String, SmtpEndpoint> KNOWN_PROVIDERS = Map.ofEntries(
            Map.entry("gmail.com", new SmtpEndpoint("smtp.gmail.com", 587)),
            Map.entry("outlook.com", new SmtpEndpoint("smtp.office365.com", 587)),
            Map.entry("hotmail.com", new SmtpEndpoint("smtp.office365.com", 587)),
            Map.entry("live.com", new SmtpEndpoint("smtp.office365.com", 587)),
            Map.entry("msn.com", new SmtpEndpoint("smtp.office365.com", 587)),
            Map.entry("yahoo.com", new SmtpEndpoint("smtp.mail.yahoo.com", 587)),
            Map.entry("yahoo.es", new SmtpEndpoint("smtp.mail.yahoo.com", 587)),
            Map.entry("icloud.com", new SmtpEndpoint("smtp.mail.me.com", 587)),
            Map.entry("me.com", new SmtpEndpoint("smtp.mail.me.com", 587)));

    private SmtpProviderDefaults() {
    }

    static String defaultHost(String email) {
        SmtpEndpoint endpoint = KNOWN_PROVIDERS.get(domainOf(email));
        return endpoint != null ? endpoint.host() : null;
    }

    static Integer defaultPort(String email) {
        SmtpEndpoint endpoint = KNOWN_PROVIDERS.get(domainOf(email));
        return endpoint != null ? endpoint.port() : null;
    }

    private static String domainOf(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at >= 0 ? email.substring(at + 1).toLowerCase(Locale.ROOT) : null;
    }
}
