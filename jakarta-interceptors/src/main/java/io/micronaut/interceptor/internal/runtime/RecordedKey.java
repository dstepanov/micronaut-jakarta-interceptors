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

import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.interceptor.metadata.InterceptionKind;
import org.jspecify.annotations.Nullable;

/**
 * An interception as the processor recorded it on an element, used to find the chain of the element again without
 * reading the interception a second time.
 *
 * <p>Reading an interception into what its chain is built from brings each of its bindings into the shape bindings are
 * compared in, which copies the members and compares them pairwise. That is too much to do every time a bean is
 * created or an invocation looks its chain up, so the chain is remembered under the interception the processor
 * wrote, and that is read into a chain only the first time it is met. Two elements recorded alike share the entry;
 * two recorded differently but equal once read are two entries holding equal chains, which costs one more build when
 * the second is first met and nothing else.</p>
 *
 * <p>Two interceptions are compared with {@link AnnotationValue#matches(AnnotationValue)}, member for member and an
 * annotation a member holds the same way, never through {@link AnnotationValue#equals(Object)}, which passes over a
 * member the other value does not have. The processor writes what it records in full, so the defaults that
 * comparison fills in add nothing either side lacks.</p>
 *
 * <p>The interception is read from the metadata Micronaut compiled: the resolver reads it from beneath the wrapper an
 * evaluated expression puts around the metadata of an invocation, so that nothing of an invocation is held here.</p>
 *
 * @param kind         The kind of interception
 * @param interception The interception as recorded, or {@code null} where the element records none
 * @param hash         The hash of the two, computed once. The members are hashed as Micronaut hashes the values of
 *                     an annotation; two interceptions that match yet hash apart are merely two entries
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record RecordedKey(InterceptionKind kind, @Nullable AnnotationValue<?> interception, int hash) {

    /**
     * Reads the key of an interception as it was recorded.
     *
     * @param kind         The kind of interception
     * @param interception The interception as recorded, or {@code null} where the element records none
     * @return The key
     */
    static RecordedKey of(InterceptionKind kind, @Nullable AnnotationValue<?> interception) {
        return new RecordedKey(kind, interception, hash(kind, hashOf(interception)));
    }

    /**
     * Reads the key of an interception as it was recorded for every kind of interception, hashing its members once.
     *
     * @param interception The interception as recorded, or {@code null} where the element records none
     * @return The keys, by the ordinal of their kind
     */
    static RecordedKey[] ofEveryKind(@Nullable AnnotationValue<?> interception) {
        int membersHash = hashOf(interception);
        InterceptionKind[] kinds = InterceptionKind.values();
        RecordedKey[] keys = new RecordedKey[kinds.length];
        for (InterceptionKind kind : kinds) {
            keys[kind.ordinal()] = new RecordedKey(kind, interception, hash(kind, membersHash));
        }
        return keys;
    }

    private static int hashOf(@Nullable AnnotationValue<?> interception) {
        return interception == null ? 0 : AnnotationUtil.calculateHashCode(interception.getValues());
    }

    private static int hash(InterceptionKind kind, int membersHash) {
        return 31 * kind.hashCode() + membersHash;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordedKey other) || hash != other.hash || kind != other.kind) {
            return false;
        }
        if (interception == null || other.interception == null) {
            return interception == null && other.interception == null;
        }
        return interception.matches(other.interception);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return kind + " " + interception;
    }
}
