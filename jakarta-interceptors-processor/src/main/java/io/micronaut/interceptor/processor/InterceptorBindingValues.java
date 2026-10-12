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
package io.micronaut.interceptor.processor;

import io.micronaut.context.annotation.NonBinding;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the interceptor bindings of an element and reduces each of them to what it is compared by.
 *
 * <p>An interceptor applies to an element when every binding annotation the interceptor declares is also declared
 * by the element, with the same member values. A member excluded from the binding, which is what
 * {@code jakarta.enterprise.util.Nonbinding} does, is left out of the comparison, and the values an annotation
 * defaults to are filled in first, so that {@code @Logged} and {@code @Logged(level = "INFO")} are the same binding
 * when {@code "INFO"} is the default.</p>
 *
 * <p>Which members take part in a binding does not depend on the running application, so it is worked out here: each
 * binding is written out on the element as the value of its annotation with the defaults filled in, the excluded
 * members left out and listed under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}, and an enum constant or a class
 * spelt as Micronaut records it, by its name. The runtime compares those values, through
 * {@code io.micronaut.interceptor.spi.InterceptorBindings}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class InterceptorBindingValues {

    private InterceptorBindingValues() {
    }

    /**
     * Makes a member value comparable to the same value read from another element.
     *
     * <p>An array does not compare by its contents, so a binding with an array-valued member would never match
     * another declaration of it. The specification leaves such a member to an extension to define; reading it as
     * the list of its elements is what makes it behave as any other member does.</p>
     *
     * <p>An annotation held as the value of a member is reduced to what it binds by, exactly as the binding that
     * holds it is: the members it defaults to filled in and the ones excluded from it left out.</p>
     *
     * <p>An enum constant is read as its name and a class as its {@link AnnotationClassValue}: Micronaut records
     * them that way in some places and as themselves in others, and a member has one type, so the two are the same
     * value. An evaluated expression is read as the expression it was written as.</p>
     *
     * <p>The array types are listed out rather than read through {@code java.lang.reflect.Array}, so that
     * comparing bindings stays as free of the reflection of the platform as the rest of the interception.</p>
     *
     * @param value    The value of a member
     * @param excluded The members excluded from a binding annotation, by its name
     * @return The value, a list of its elements when it is an array, or the binding it is reduced to when it is an
     * annotation
     */
    private static @Nullable Object normalize(@Nullable Object value, ExcludedMembers excluded) {
        return switch (value) {
            case null -> null;
            case AnnotationValue<?> annotation -> of(annotation, excluded);
            case Enum<?> constant -> constant.name();
            case Class<?> type -> new AnnotationClassValue<>(type.getName());
            case EvaluatedExpressionReference expression -> normalize(expression.annotationValue(), excluded);
            case Object[] array -> Arrays.stream(array).map(element -> normalize(element, excluded)).toList();
            case int[] array -> Arrays.stream(array).boxed().toList();
            case long[] array -> Arrays.stream(array).boxed().toList();
            case double[] array -> Arrays.stream(array).boxed().toList();
            case boolean[] array -> booleans(array);
            case byte[] array -> bytes(array);
            case short[] array -> shorts(array);
            case char[] array -> chars(array);
            case float[] array -> floats(array);
            default -> value;
        };
    }

    private static List<Object> booleans(boolean[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (boolean element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> bytes(byte[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (byte element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> shorts(short[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (short element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> chars(char[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (char element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> floats(float[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (float element : array) {
            values.add(element);
        }
        return values;
    }

    /**
     * Reads the bindings an element declares, with the members excluded from each of them left out however the
     * element declared it.
     *
     * @param annotationMetadata The metadata of the element
     * @param excluded           The members excluded from a binding annotation, by its name
     * @return The bindings
     */
    public static Set<Binding> of(AnnotationMetadata annotationMetadata, ExcludedMembers excluded) {
        List<String> names =
            annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptorSupport.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return Set.of();
        }
        Set<Binding> bindings = new LinkedHashSet<>(names.size());
        for (String name : names) {
            // a repeatable binding binds by every occurrence of it the element carries, each of which is a binding
            // of its own: an interceptor declaring one of them is bound to the element, and one declaring an
            // occurrence the element does not carry is not. Reading the annotation by name answers with the first
            // occurrence alone, which made the order they were declared in decide what an element binds by
            List<AnnotationValue<Annotation>> occurrences = annotationMetadata.getAnnotationValuesByName(name);
            if (occurrences.isEmpty()) {
                annotationMetadata.findAnnotation(name)
                    .map(annotation -> of(annotation, excluded))
                    .ifPresent(bindings::add);
                continue;
            }
            for (AnnotationValue<Annotation> occurrence : occurrences) {
                bindings.add(of(occurrence, excluded));
            }
        }
        return bindings;
    }

    /**
     * Reduces one binding annotation to what it binds by, going by the members it records as excluded from the
     * binding.
     *
     * <p>Used at compilation time as well, so that the conflict between two declarations of a binding is decided
     * by exactly what decides whether an interceptor is bound at runtime.</p>
     *
     * @param annotation The binding annotation
     * @return The binding
     */
    public static Binding of(AnnotationValue<?> annotation) {
        return of(annotation, ExcludedMembers.AS_RECORDED);
    }

    /**
     * Reduces one binding annotation to what it binds by.
     *
     * <p>Micronaut records a member excluded from a binding only on an occurrence of the annotation that supplied a
     * value for that member, so an occurrence that left every excluded member to its default records none of them
     * and would otherwise be compared by values that take no part in the binding. The members excluded from the
     * annotation type are therefore supplied as well, and this is the one place a binding is reduced, so that two
     * occurrences are compared by the same members wherever either of them is read from.</p>
     *
     * @param annotation The binding annotation
     * @param excluded   The members excluded from a binding annotation, by its name
     * @return The binding
     */
    public static Binding of(AnnotationValue<?> annotation, ExcludedMembers excluded) {
        String name = annotation.getAnnotationName();
        // the keys are read as strings so that two bindings compare by the names of their members, which a
        // CharSequence does not promise to do
        Map<String, Object> members = new LinkedHashMap<>();
        Set<String> defaulted = new HashSet<>();
        Map<CharSequence, Object> defaults = annotation.getDefaultValues();
        if (defaults != null) {
            defaults.forEach((member, value) -> {
                members.put(member.toString(), value);
                defaulted.add(member.toString());
            });
        }
        annotation.getValues().forEach((member, value) -> members.put(member.toString(), value));
        // an empty string no default was recorded for is left out, which is what makes a default of "" compare
        // equal whether it is declared or not; see isEmptyString
        members.entrySet().removeIf(entry -> !defaulted.contains(entry.getKey()) && isEmptyString(entry.getValue()));
        Set<String> nonBinding =
            new TreeSet<>(Arrays.asList(annotation.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)));
        nonBinding.addAll(excluded.of(name));
        members.keySet().removeAll(nonBinding);
        members.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        members.remove(AnnotationUtil.STEREOTYPES_MEMBER);
        Map<String, Object> values = new LinkedHashMap<>(members.size());
        Map<CharSequence, Object> written = new LinkedHashMap<>(members.size() + 1);
        members.forEach((member, value) -> {
            values.put(member, normalize(value, excluded));
            written.put(member, writable(value, excluded));
        });
        // the excluded members are listed for whatever the binding is compared with at runtime, which may be the
        // value of an annotation instance that carries all of its members and says nothing of which bind
        if (!nonBinding.isEmpty()) {
            written.put(AnnotationUtil.NON_BINDING_ATTRIBUTE, nonBinding.toArray(String[]::new));
        }
        return new Binding(name, values, AnnotationValue.builder(name).members(written).build());
    }

    /**
     * Spells a member value the way the binding is written out: as Micronaut records it, an enum constant by its
     * name and a class as an {@link AnnotationClassValue}, with an annotation it holds written out as a binding is.
     * An array keeps its type, which is what Micronaut writes the metadata of an annotation from.
     *
     * @param value    The value of a member
     * @param excluded The members excluded from a binding annotation, by its name
     * @return The value to write
     */
    private static Object writable(Object value, ExcludedMembers excluded) {
        if (value instanceof AnnotationValue<?> annotation) {
            return of(annotation, excluded).written();
        }
        if (value instanceof AnnotationValue<?>[] annotations) {
            AnnotationValue<?>[] written = new AnnotationValue<?>[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                written[i] = of(annotations[i], excluded).written();
            }
            return written;
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        if (value instanceof Enum<?>[] constants) {
            return Arrays.stream(constants).map(Enum::name).toArray(String[]::new);
        }
        if (value instanceof Class<?> type) {
            return new AnnotationClassValue<>(type.getName());
        }
        if (value instanceof Class<?>[] types) {
            return Arrays.stream(types)
                .map(type -> new AnnotationClassValue<>(type.getName()))
                .toArray(AnnotationClassValue<?>[]::new);
        }
        if (value instanceof EvaluatedExpressionReference expression) {
            return writable(expression.annotationValue(), excluded);
        }
        return value;
    }

    /**
     * Reads the members excluded from a binding annotation off the annotation type itself, remembering what it read:
     * the same annotation is reached as often as it is declared, and it is the same annotation every time.
     *
     * @param context The visitor context, which resolves the annotation types
     * @return The excluded members of any binding annotation the compilation can see
     */
    public static ExcludedMembers excludedMembersOf(VisitorContext context) {
        Map<String, List<String>> read = new HashMap<>();
        return name -> read.computeIfAbsent(name, annotationName -> {
            ClassElement annotationType = context.getClassElement(annotationName).orElse(null);
            if (annotationType == null) {
                return List.of();
            }
            return annotationType.getEnclosedElements(ElementQuery.ALL_METHODS)
                .stream()
                .filter(member -> member.hasAnnotation(JakartaInterceptors.NONBINDING)
                    || member.hasAnnotation(NonBinding.class))
                .map(MethodElement::getName)
                .toList();
        });
    }

    /**
     * Tells whether a member value is the empty string, which is the one default that Micronaut leaves out of
     * the defaults it records for an annotation compiled from Java or Kotlin.
     *
     * <p>A default is filled in before two bindings are compared, and that only works for a default Micronaut
     * recorded. Leaving out a member that defaults to {@code ""} means {@code @Region("")} carries a
     * {@code value} that {@code @Region} does not, and the two would never bind to each other, although they are
     * the same binding.</p>
     *
     * <p>The member values are what is left to go by, and an empty string declared for a member with no recorded
     * default is left out of the comparison. That is exactly right for each member it can happen to: one whose
     * default is {@code ""} reads the same whether the value is declared or not, and one without any default is
     * declared by every declaration of the binding, so leaving the empty value out on both sides of the comparison
     * changes nothing about whether they are equal. A member whose default Micronaut did record keeps an empty
     * string, which differs from that default.</p>
     *
     * <p>Reading the defaults off the annotation type instead does not work for every language: what the visitor
     * context offers for it answers all of them for Java, none for Kotlin, and for Groovy only the ones that are
     * a single constant.</p>
     *
     * @param value The value of a member
     * @return Whether the value is an empty string
     */
    private static boolean isEmptyString(@Nullable Object value) {
        return value instanceof String string && string.isEmpty();
    }

    /**
     * The members excluded from a binding annotation, answered by the name of the annotation.
     *
     * <p>What an occurrence of an annotation records is not the whole of it: Micronaut records an excluded member
     * only where a value was supplied for it. The complete set is a property of the annotation type, which the
     * compilation can read and the runtime cannot, so it is supplied to wherever a binding is reduced.</p>
     */
    public interface ExcludedMembers {

        /**
         * What an occurrence of an annotation records itself, which is all there is to go by once the binding has
         * been written out.
         */
        ExcludedMembers AS_RECORDED = name -> List.of();

        /**
         * The members excluded from a binding annotation.
         *
         * @param annotationName The name of the annotation
         * @return The names of the excluded members, which is empty for an annotation excluding none
         */
        List<String> of(String annotationName);
    }

    /**
     * A binding annotation reduced to what it binds by: its name and the member values that take part in the
     * binding, together with the value it is written out as.
     *
     * <p>Two bindings are equal when their names and their member values are; the value written out follows from
     * those and takes no part. The member values hold an array as the list of its elements, so that two of them
     * compare by their contents.</p>
     *
     * @param name    The annotation name
     * @param values  The member values
     * @param written The binding as it is written out on an element, named by its annotation
     */
    public record Binding(String name, Map<String, Object> values, AnnotationValue<?> written) {

        @Override
        public boolean equals(@Nullable Object o) {
            return this == o || (o instanceof Binding other && name.equals(other.name) && values.equals(other.values));
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + values.hashCode();
        }
    }
}
