package com.luispuchol.selfbill.selfbill_api.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.validation.Constraint;
import jakarta.validation.Valid;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SelfBill API", version = "v1", description = """
        Self-hosted, single-user invoicing API.

        Errors are returned as RFC 9457 problem details with an `errorCode` property.
        Deletion is always logical: deleted rows disappear from the API but documents keep referencing them."""))
public class OpenApiConfig {

    private static final String PROBLEM_SCHEMA = "Problem";

    @Bean
    OpenApiCustomizer problemSchema() {
        return openApi -> ModelConverters.getInstance().read(Problem.class)
                .forEach(openApi.getComponents()::addSchemas);
    }

    @Bean
    OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            List<ErrorCode> errors = new ArrayList<>();
            if (validatesInput(handlerMethod)) {
                errors.add(ErrorCode.VALIDATION_ERROR);
            }
            ApiErrors apiErrors = handlerMethod.getMethodAnnotation(ApiErrors.class);
            if (apiErrors != null) {
                errors.addAll(List.of(apiErrors.value()));
            }

            Map<Integer, List<ErrorCode>> errorsByStatus = errors.stream()
                    .collect(Collectors.groupingBy(error -> error.getHttpStatus().value(), TreeMap::new,
                            Collectors.toList()));
            errorsByStatus.forEach((status, codes) -> operation.getResponses()
                    .addApiResponse(String.valueOf(status), errorResponse(HttpStatus.valueOf(status), codes)));
            return operation;
        };
    }

    private static boolean validatesInput(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                .flatMap(parameter -> Arrays.stream(parameter.getParameterAnnotations()))
                .anyMatch(annotation -> annotation instanceof Valid
                        || annotation.annotationType().isAnnotationPresent(Constraint.class));
    }

    private static ApiResponse errorResponse(HttpStatus status, List<ErrorCode> codes) {
        String description = codes.stream()
                .map(code -> "- `" + code.name() + "`: " + code.format())
                .collect(Collectors.joining("\n", status.getReasonPhrase() + "\n\n", ""));
        var problem = new io.swagger.v3.oas.models.media.MediaType()
                .schema(new io.swagger.v3.oas.models.media.Schema<>().$ref(PROBLEM_SCHEMA));
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE, problem));
    }

    @Schema(name = PROBLEM_SCHEMA,
            description = "RFC 9457 problem detail. With `VALIDATION_ERROR`, `errors` holds the message of each invalid field")
    record Problem(
            @Schema(example = "about:blank")
            String type,

            @Schema(example = "Not Found")
            String title,

            @Schema(example = "404")
            int status,

            @Schema(example = "Client not found: 7")
            String detail,

            @Schema(example = "/api/clients/7")
            String instance,

            ErrorCode errorCode,

            Map<String, String> errors) {
    }
}
