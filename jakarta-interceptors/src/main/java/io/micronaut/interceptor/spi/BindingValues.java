/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.interceptor.spi;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.internal.metadata.BindingValue;

/**
 * One interceptor binding compared with another, exactly as {@link InterceptorBindings} compares the bindings of an
 * interceptor with those of an interception point.
 *
 * <p>It is for a module that compares bindings one at a time rather than as the sets an interceptor and an
 * interception point declare - as Contexts and Dependency Injection does, telling whether two interceptor bindings
 * are equivalent, hashing one, or comparing the annotations of a qualifier that, as a binding does, leaves out the
 * members marked {@code jakarta.enterprise.util.Nonbinding}. Comparing them here is what keeps its answers the same as
 * the ones the chains of this module are built from.</p>
 *
 * <p>A value is brought into the shape it is compared in by these rules:</p>
 * <ul>
 *     <li>the members it leaves out are filled in from the defaults it carries,
 *     {@link AnnotationValue#getDefaultValues()}, and from nothing else;</li>
 *     <li>the members it lists as excluded from the binding under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE} are
 *     left out, and two values are compared without the members either of them lists;</li>
 *     <li>a member that is an empty string is compared as if it were left out, once the defaults are filled in.
 *     Micronaut records no default of an empty string for an annotation compiled from Java or Kotlin, so
 *     {@code @Region("")} and {@code @Region} are the same binding where the member defaults to {@code ""}; where it
 *     defaults to another value, {@code @Region("")} is not {@code @Region}, which has that default filled in;</li>
 *     <li>the two members Micronaut reserves, {@value AnnotationUtil#NON_BINDING_ATTRIBUTE} and
 *     {@value AnnotationUtil#STEREOTYPES_MEMBER}, are left out. Any other member binds, whatever its name: a member
 *     of an annotation may be named with a {@code $} as any other identifier may;</li>
 *     <li>an enum constant is its name, a class is an {@link AnnotationClassValue}, an array is compared by its
 *     elements in order, an annotation held by a member is compared as a binding is, and an evaluated expression is
 *     the expression it was written as.</li>
 * </ul>
 *
 * <p>{@link AnnotationValue#equals(Object)} compares none of this: it passes over a member the other value does not
 * have, so two values declaring different members of the same count compare equal.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public final class BindingValues {

    private BindingValues() {
    }

    /**
     * Brings the value of a binding annotation into the shape it is compared in: the members that bind, with the
     * defaults the value carries filled in, spelt as Micronaut records them, and the members excluded from the
     * binding listed under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}, as the processor records a binding.
     *
     * <p>The value returned is the caller's, a new one on every call. Brought into shape again, it is the same.</p>
     *
     * @param value The value of a binding annotation, read from metadata or built from an annotation instance
     * @return The value in the shape it is compared in
     */
    public static AnnotationValue<?> normalize(AnnotationValue<?> value) {
        return BindingValue.of(value).toAnnotationValue();
    }

    /**
     * Tells whether two values of binding annotations are the same binding: the same annotation, whose members
     * that bind are equal once both are brought into the shape they are compared in, without the members either of
     * them lists as excluded.
     *
     * @param value The value of one binding annotation
     * @param other The value of the other
     * @return Whether the two are the same binding
     */
    public static boolean matches(AnnotationValue<?> value, AnnotationValue<?> other) {
        return BindingValue.of(value).matches(BindingValue.of(other));
    }

    /**
     * A hash code of the value of a binding annotation that two values which {@link #matches(AnnotationValue,
     * AnnotationValue) match} share.
     *
     * <p>It is the hash code of the name of the annotation alone. Two values are compared without the members either
     * of them lists as excluded, so a member one of them binds by may take no part in the comparison; and Micronaut
     * lists an excluded member only on a declaration that gives it a value. No member is therefore hashed, as none is
     * certain to take part.</p>
     *
     * @param value The value of a binding annotation
     * @return The hash code
     */
    public static int hashCode(AnnotationValue<?> value) {
        return value.getAnnotationName().hashCode();
    }
}
