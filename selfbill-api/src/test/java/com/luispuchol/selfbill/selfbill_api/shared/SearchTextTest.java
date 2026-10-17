package com.luispuchol.selfbill.selfbill_api.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTextTest {

    @Test
    void stripsAccentsAndLowercases() {
        assertThat(SearchText.normalize("García")).isEqualTo("garcia");
        assertThat(SearchText.normalize("GARCÍA")).isEqualTo("garcia");
        assertThat(SearchText.normalize("Peña")).isEqualTo("pena");
    }

    @Test
    void joinsNonNullParts() {
        assertThat(SearchText.normalize("Bar Pepe", null, "B1")).isEqualTo("bar pepe b1");
    }
}
