package io.micronaut.interceptor.test.binding

import jakarta.interceptor.InterceptorBinding

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * A binding with a default for its member, which a method may declare without giving the member a value.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Zone {

    String value() default "z"
}
