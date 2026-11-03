package com.luispuchol.selfbill.selfbill_api.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Creates the folder of the SQLite file before the DataSource opens it: on a new machine ~/.selfbill does not
 * exist yet, and sqlite-jdbc fails (inside Flyway) instead of creating it. An EnvironmentPostProcessor runs
 * before any bean, and sees the URL however it was set (properties, launcher argument).
 */
public class SqliteFolderInitializer implements EnvironmentPostProcessor {

    private static final String PREFIX = "jdbc:sqlite:";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        createFolderOf(environment.getProperty("spring.datasource.url"));
    }

    static void createFolderOf(String url) {
        if (url == null || !url.startsWith(PREFIX)) {
            return;
        }
        String path = url.substring(PREFIX.length());
        int options = path.indexOf('?');
        if (options >= 0) {
            path = path.substring(0, options);
        }
        // In-memory databases, and URIs (file:...?mode=memory), have no folder of their own
        if (path.isEmpty() || path.startsWith(":memory:") || path.startsWith("file:")) {
            return;
        }
        Path parent = Path.of(path).getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the database folder " + parent, e);
        }
    }
}
