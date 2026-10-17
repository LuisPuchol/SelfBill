package com.luispuchol.selfbill.selfbill_api.shared;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Builds the text the search columns hold: no accents, lower case. */
public final class SearchText {

    private SearchText() {
    }

    // SQLite's lower() only folds ASCII, so the folding is done here and stored in search_text
    public static String normalize(String... parts) {
        String joined = Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
        return Normalizer.normalize(joined, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
