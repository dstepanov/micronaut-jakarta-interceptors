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
package io.micronaut.interceptor.runtime;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ExecutableMethod;
import jakarta.interceptor.InvocationContext;
import org.jspecify.annotations.Nullable;

/**
 * One interceptor class of a chain, together with the interceptor method that interposes on the kind of
 * interception the chain was resolved for.
 *
 * <p>The method is the executable method Micronaut generated for it at compilation time, so invoking an interceptor
 * method costs a virtual call rather than a reflective one.</p>
 *
 * @param interceptorClass The interceptor class
 * @param method     The interceptor method
 * @param self       Whether the method is declared by the intercepted class itself, in which case it is invoked on
 *                   the intercepted instance rather than on an interceptor of its own
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record InterceptorReference(Class<?> interceptorClass, ExecutableMethod<Object, Object> method, boolean self) {

    /**
     * Invokes the interceptor method.
     *
     * @param interceptor The interceptor instance
     * @param context     The context to pass to it
     * @return Whatever the interceptor method returned, which is the result of the invocation for an
     * {@code @AroundInvoke} method and nothing for the others
     */
    @Nullable Object invoke(Object interceptor, InvocationContext context) {
        return method.invoke(interceptor, context);
    }

}
