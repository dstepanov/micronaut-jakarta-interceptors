package io.micronaut.interceptor.test.binding

import jakarta.enterprise.util.Nonbinding
import jakarta.interceptor.InterceptorBinding

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * The annotation the single member of {@link Governed} holds, with a member that does not bind. It is not a binding
 * itself, so Micronaut records the members excluded from it where it is held.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([])
@interface Rule {
    String name()

    @Nonbinding
    String note() default ""
}

/**
 * The annotation the array member of {@link Governed} holds, with a member that does not bind.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([])
@interface Clause {
    String name()

    @Nonbinding
    String note() default ""
}

/** A binding whose members are an annotation and an array of another. */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Governed {
    Rule primary()

    Clause[] clauses() default []
}
