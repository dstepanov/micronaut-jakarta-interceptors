package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding with a member of each primitive array type. Each is a separate branch of the normalisation that makes
 * an array compare by its contents, and a separate way for two declarations of the same values to compare unequal.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Primitives {

    boolean[] bools() default {};

    byte[] bytes() default {};

    short[] shorts() default {};

    char[] chars() default {};

    int[] ints() default {};

    long[] longs() default {};

    float[] floats() default {};

    double[] doubles() default {};
}
