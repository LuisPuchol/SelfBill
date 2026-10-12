package com.luispuchol.selfbill.selfbill_api.billing.document;

import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;

public record OutgoingDocument(Client recipient, String number, byte[] pdf) {
}
