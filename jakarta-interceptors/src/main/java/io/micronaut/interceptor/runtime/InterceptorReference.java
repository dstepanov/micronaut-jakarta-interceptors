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
import io.micronaut.core.reflect.exception.InvocationException;
import io.micronaut.inject.ExecutableMethod;
import jakarta.interceptor.InvocationContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

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
 * @param reflective Whether the executable method reaches the interceptor method reflectively, which is the case
 *                   for a method generated code cannot call
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record InterceptorReference(Class<?> interceptorClass,
                            ExecutableMethod<Object, Object> method,
                            boolean self,
                            boolean reflective) {

    /**
     * Invokes the interceptor method.
     *
     * <p>A private interceptor method is one the executable method cannot call directly, and reaches reflectively
     * instead, which wraps whatever the method threw in an {@link InvocationException} caused by an
     * {@code InvocationTargetException}. That envelope is taken off: what the method threw travels on as it was
     * thrown, to the interceptors before it and to the caller, the same as it would from a method of any other
     * access. A checked exception is rethrown unchanged too, as it is from an interceptor method called directly;
     * which checked exceptions are allowed through is decided by the kind of interception, further up.</p>
     *
     * <p>Only a method that is reached reflectively has it taken off. An interceptor method reached directly threw
     * whatever it threw itself, and an interceptor is free to throw that pair of exceptions on purpose; unwrapping
     * on the shape of the exception alone would hand the caller the inside of something the interceptor meant to
     * throw whole.</p>
     *
     * @param interceptor The interceptor instance
     * @param context     The context to pass to it
     * @return Whatever the interceptor method returned, which is the result of the invocation for an
     * {@code @AroundInvoke} method and nothing for the others
     */
    @Nullable Object invoke(Object interceptor, InvocationContext context) {
        if (!reflective) {
            return method.invoke(interceptor, context);
        }
        try {
            return method.invoke(interceptor, context);
        } catch (InvocationException e) {
            Throwable thrown = e.getCause() instanceof InvocationTargetException target ? target.getCause() : null;
            if (thrown == null) {
                // not the envelope of a reflective call: an exception of its own, which travels as it is
                throw e;
            }
            throw sneakyThrow(thrown);
        }
    }

    /**
     * Rethrows an exception as it is, checked or not.
     *
     * @param e   The exception
     * @param <E> The type the exception is rethrown as
     * @return Never returns; declared so that the call site can be written as a {@code throw}
     * @throws E The exception
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
