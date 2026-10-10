package com.luispuchol.selfbill.selfbill_api;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void modulesDependOnEachOtherInOneDirection() {
        ApplicationModules.of(SelfbillApiApplication.class).verify();
    }
}
