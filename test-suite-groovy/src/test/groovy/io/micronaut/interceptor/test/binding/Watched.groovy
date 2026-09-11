package io.micronaut.interceptor.test.binding

import jakarta.interceptor.InterceptorBinding

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * A binding that other annotations carry to what they are declared on.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE])
@interface Watched {
}
