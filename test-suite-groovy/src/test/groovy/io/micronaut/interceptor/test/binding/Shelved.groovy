package io.micronaut.interceptor.test.binding

import jakarta.interceptor.InterceptorBinding

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * A binding whose member defaults to an empty string.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Shelved {

    String value() default ""
}
