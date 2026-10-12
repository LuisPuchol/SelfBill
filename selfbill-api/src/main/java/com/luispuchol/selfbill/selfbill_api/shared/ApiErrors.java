package com.luispuchol.selfbill.selfbill_api.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Business errors an endpoint can answer with, documented in the OpenAPI spec by {@link OpenApiConfig}.
 * {@link ErrorCode#VALIDATION_ERROR} is added on its own when the endpoint validates its input.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrors {

    ErrorCode[] value();
}
