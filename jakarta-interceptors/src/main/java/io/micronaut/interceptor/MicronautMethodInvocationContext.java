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
package io.micronaut.interceptor;

import io.micronaut.inject.ExecutableMethod;

/**
 * The {@link MicronautInvocationContext} of an interception of a method: a business method, a timeout method or a
 * lifecycle callback.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public interface MicronautMethodInvocationContext extends MicronautInvocationContext {

    /**
     * The executable method Micronaut generated for the intercepted method at compilation time.
     *
     * <p>It describes what {@link #getMethod()} describes - the declaring type, the name, the arguments and the
     * annotation metadata - and invoking it dispatches through generated code. Reading it reflects on nothing.</p>
     *
     * <p>For a lifecycle callback this is the callback the chain of the event was started for, which is the first
     * one the bean runs; {@link #getMethod()} answers with the callback of the intercepted class itself, as the
     * specification describes.</p>
     *
     * @return The executable method, never {@code null}
     */
    ExecutableMethod<Object, Object> getExecutableMethod();
}
