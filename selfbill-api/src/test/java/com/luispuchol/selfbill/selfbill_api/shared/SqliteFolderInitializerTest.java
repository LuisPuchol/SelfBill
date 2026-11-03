package com.luispuchol.selfbill.selfbill_api.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class SqliteFolderInitializerTest {

    @TempDir
    Path tmp;

    @Test
    void createsTheMissingFolder() {
    Path folder = tmp.resolve("a").resolve("b");

    SqliteFolderInitializer.createFolderOf("jdbc:sqlite:" + folder.resolve("x.db") + "?foreign_keys=true");

    assertThat(folder).isDirectory();
    }

    @Test
    void leavesMemoryAndBareUrlsAlone() throws IOException {
    assertThatCode(() -> {
        SqliteFolderInitializer.createFolderOf("jdbc:sqlite::memory:");
        SqliteFolderInitializer.createFolderOf("jdbc:sqlite:file:x?mode=memory&cache=shared");
        SqliteFolderInitializer.createFolderOf("jdbc:sqlite:x.db");
        SqliteFolderInitializer.createFolderOf("jdbc:sqlite:./x.db?foreign_keys=true");
        SqliteFolderInitializer.createFolderOf("jdbc:postgresql://host/" + tmp.resolve("never") + "/x");
        SqliteFolderInitializer.createFolderOf(null);
    }).doesNotThrowAnyException();

    try (var entries = Files.list(tmp)) {
        assertThat(entries).isEmpty();
    }
    }

    @Test
    void defaultUrlIsUnderUserHome() throws IOException {
    Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
    String url = properties.getProperty("spring.datasource.url").replace("${user.home}", "HOME");

    assertThat(url).isEqualTo(
        "jdbc:sqlite:HOME/.selfbill/selfbill.db?foreign_keys=true&journal_mode=WAL&transaction_mode=IMMEDIATE");
    }
}
