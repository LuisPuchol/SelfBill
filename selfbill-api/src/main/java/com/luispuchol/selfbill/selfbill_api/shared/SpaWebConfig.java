package com.luispuchol.selfbill.selfbill_api.shared;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** Serves the built front from the jar; client-side routes fall back to index.html. */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // File names under _app/immutable carry a content hash, so they can be cached for good
        registry.addResourceHandler("/_app/immutable/**")
                .addResourceLocations("classpath:/static/_app/immutable/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    /**
     * The resource handler never sees "/" (its path is empty) and Boot's welcome page mapping only
     * forwards it, so the root is served directly.
     */
    @Controller
    static class RootController {

        @GetMapping("/")
        ResponseEntity<Resource> index() {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noCache())
                    .body(new ClassPathResource("static/index.html"));
        }
    }

    private static class SpaFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            // A missing API route or build file must stay a 404 instead of answering with HTML
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("v3/")
                    || resourcePath.startsWith("swagger-ui") || hasExtension(resourcePath)) {
                return null;
            }
            return new ClassPathResource("static/index.html");
        }

        private static boolean hasExtension(String path) {
            return path.substring(path.lastIndexOf('/') + 1).contains(".");
        }
    }
}
