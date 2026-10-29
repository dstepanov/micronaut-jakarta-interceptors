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
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.metadata.InterceptionKind;
import org.jspecify.annotations.Nullable;

/**
 * The interception the processor recorded on a method, read once and kept by the method itself.
 *
 * <p>Finding the chain of a method under its {@link RecordedKey} hashes the interception, and compares it member
 * for member with the key the chain was remembered under. A method is invoked as a business method, and its bean's
 * lifecycle events run through it, for every bean created of a prototype, so the key of each kind is built here once
 * and handed to every lookup. The resolver remembers a chain under the very key it is later asked for, which a
 * lookup then finds without hashing or comparing anything.</p>
 *
 * <p>What is kept is derived from the method's compiled metadata alone: no chain, bean or other state of a context.
 * The chains themselves depend on the beans of a context, and stay with the resolver of each. It is read from the
 * unevaluated metadata, beneath the wrapper an evaluated expression puts around it, so it is kept even for a method
 * another of whose annotations holds an expression.</p>
 *
 * <p>It is kept through {@link ExecutableMethod#getCachedUnevaluatedValue}, as the interception holds no evaluated
 * expression and is read beneath the wrapper one puts around the metadata: a method an annotation of which holds one
 * keeps it as well.</p>
 *
 * @param interception The interception as recorded, or {@code null} where the method records none
 * @param timeout      Whether the method is a timeout method when the scheduler invokes it
 * @param keys         The key of the interception for each kind, by the ordinal of the kind
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record MethodInterception(@Nullable AnnotationValue<JakartaInterception> interception,
                          boolean timeout,
                          RecordedKey[] keys) {

    /**
     * Reads the interception the processor recorded on a method.
     *
     * @param method The method
     * @return What is kept of it
     */
    static MethodInterception of(ExecutableMethod<?, ?> method) {
        AnnotationValue<JakartaInterception> interception = InterceptorChainResolver.recordedInterception(
            method.getAnnotationMetadata());
        InterceptionKind[] kinds = InterceptionKind.values();
        RecordedKey[] keys = new RecordedKey[kinds.length];
        for (InterceptionKind kind : kinds) {
            keys[kind.ordinal()] = RecordedKey.of(kind, interception);
        }
        boolean timeout = interception != null && interception.booleanValue(JakartaInterception.TIMEOUT).orElse(false);
        return new MethodInterception(interception, timeout, keys);
    }

    /**
     * @param kind The kind of interception
     * @return The key of the interception of that kind
     */
    RecordedKey key(InterceptionKind kind) {
        return keys[kind.ordinal()];
    }
}
