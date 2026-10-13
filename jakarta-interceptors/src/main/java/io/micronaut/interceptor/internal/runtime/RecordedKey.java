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
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.interceptor.metadata.InterceptionKind;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * An interception exactly as the processor recorded it on an element, used to find the chain of the element again
 * without reading the interception a second time.
 *
 * <p>Reading an interception into what its chain is built from brings each of its bindings into the shape bindings are
 * compared in, which copies the members and compares them pairwise. That is too much to do every time a bean is
 * created or an invocation looks its chain up, so the chain is remembered under the members the processor wrote as
 * well, and those are read into a {@link InterceptorChainResolver.ChainKey} only the first time they are met. Two
 * elements recorded alike share the entry; two recorded differently but equal once read are two entries for one
 * chain, which costs one more read and nothing else.</p>
 *
 * <p>The members are compared as they were written, member for member and element for element, and never through
 * {@link AnnotationValue#equals(Object)}, which passes over a member the other value does not have, nor through the
 * defaults of an annotation, which the registry Micronaut keeps them in may answer differently once an annotation of
 * the type has been built reflectively. The processor writes what it records in full, so nothing is left for
 * defaults to fill in.</p>
 *
 * <p>The members are those of the metadata Micronaut compiled: the resolver reads them from beneath the wrapper an
 * evaluated expression puts around the metadata of an invocation, so that nothing of an invocation is held here.</p>
 *
 * @param kind    The kind of interception, {@code null} for the interceptor classes associated with a class
 * @param members The members of the interception as recorded, or {@code null} where the element records none
 * @param hash    The hash of the two, computed once
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record RecordedKey(@Nullable InterceptionKind kind, @Nullable Map<CharSequence, Object> members, int hash) {

    /**
     * Reads the key of an interception as it was recorded.
     *
     * @param kind         The kind of interception, {@code null} for the interceptor classes associated with a class
     * @param interception The interception as recorded, or {@code null} where the element records none
     * @return The key
     */
    static RecordedKey of(@Nullable InterceptionKind kind, @Nullable AnnotationValue<?> interception) {
        Map<CharSequence, Object> members = interception == null ? null : interception.getValues();
        return new RecordedKey(kind, members, 31 * Objects.hashCode(kind) + (members == null ? 0 : hashOf(members)));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordedKey other) || hash != other.hash || kind != other.kind) {
            return false;
        }
        if (members == null || other.members == null) {
            return members == null && other.members == null;
        }
        return sameMembers(members, other.members);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return kind + " " + members;
    }

    /**
     * The members are hashed in no particular order, as they are compared.
     */
    private static int hashOf(Map<CharSequence, Object> members) {
        int hash = 0;
        for (Map.Entry<CharSequence, Object> member : members.entrySet()) {
            hash += member.getKey().toString().hashCode() ^ hashOf(member.getValue());
        }
        return hash;
    }

    private static int hashOf(@Nullable Object value) {
        return switch (value) {
            case null -> 0;
            case AnnotationValue<?> annotation -> 31 * annotation.getAnnotationName().hashCode()
                + hashOf(annotation.getValues());
            case Object[] array -> {
                int hash = 1;
                for (Object element : array) {
                    hash = 31 * hash + hashOf(element);
                }
                yield hash;
            }
            // an array of a primitive type hashes by its contents, as it is compared
            case int[] array -> Arrays.hashCode(array);
            case long[] array -> Arrays.hashCode(array);
            case double[] array -> Arrays.hashCode(array);
            case boolean[] array -> Arrays.hashCode(array);
            case byte[] array -> Arrays.hashCode(array);
            case short[] array -> Arrays.hashCode(array);
            case char[] array -> Arrays.hashCode(array);
            case float[] array -> Arrays.hashCode(array);
            // what is left is a constant, a string or a class value, each of which hashes by its contents
            default -> value.hashCode();
        };
    }

    // compiled metadata keys its members by strings; a member keyed otherwise is merely not found, which reads the
    // two as different and costs one more read of the interception
    @SuppressWarnings("CollectionUndefinedEquality")
    private static boolean sameMembers(Map<CharSequence, Object> members, Map<CharSequence, Object> others) {
        if (members.size() != others.size()) {
            return false;
        }
        for (Map.Entry<CharSequence, Object> member : members.entrySet()) {
            String name = member.getKey().toString();
            if (!others.containsKey(name) || !same(member.getValue(), others.get(name))) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(@Nullable Object value, @Nullable Object other) {
        if (value instanceof AnnotationValue<?> annotation) {
            return other instanceof AnnotationValue<?> otherAnnotation
                && annotation.getAnnotationName().equals(otherAnnotation.getAnnotationName())
                && sameMembers(annotation.getValues(), otherAnnotation.getValues());
        }
        if (value instanceof Object[] array) {
            if (!(other instanceof Object[] otherArray) || array.length != otherArray.length) {
                return false;
            }
            for (int i = 0; i < array.length; i++) {
                if (!same(array[i], otherArray[i])) {
                    return false;
                }
            }
            return true;
        }
        return Objects.deepEquals(value, other);
    }
}
