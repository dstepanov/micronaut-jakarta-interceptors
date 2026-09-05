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
 * The {@link MicronautInvocationContext} of the interception of a method: a business method, a timeout method, or
 * a lifecycle callback of a bean.
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see MicronautConstructorInvocationContext
 */
public interface MicronautMethodInvocationContext extends MicronautInvocationContext {

    /**
     * The executable method Micronaut generated for the intercepted method while the application was compiled.
     *
     * <p>It describes what {@link #getMethod()} describes - the declaring type, the name, the arguments and the
     * annotation metadata of the method - and reading any of that reflects on nothing. Invoking it dispatches
     * through generated code rather than through {@link java.lang.reflect.Method#invoke}, which is how the
     * interception invokes the intercepted method itself.</p>
     *
     * <pre>{@code
     * ExecutableMethod<?, ?> method = micronaut.getExecutableMethod();
     * method.getDeclaringType();      // the class that declared it
     * method.getMethodName();         // its name
     * method.getArguments();          // its arguments, with their generic types
     * method.getReturnType();         // what it returns
     * }</pre>
     *
     * <p>For a lifecycle callback this is the callback the chain of the event was started for, which is the first
     * one the bean runs - the one its most distant superclass declares. {@link #getMethod()} answers instead with
     * the callback of the intercepted class itself, which is the one the specification describes. Where a bean
     * declares a callback and inherits none, the two are the same method.</p>
     *
     * @return The executable method, never {@code null}
     */
    ExecutableMethod<Object, Object> getExecutableMethod();
}
