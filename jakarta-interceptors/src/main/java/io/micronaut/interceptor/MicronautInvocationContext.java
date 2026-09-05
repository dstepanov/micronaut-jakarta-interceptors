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

import io.micronaut.aop.InterceptorKind;
import io.micronaut.core.annotation.AnnotationMetadata;
import jakarta.interceptor.InvocationContext;

/**
 * What Micronaut knows about an interception, offered alongside what the specification describes.
 *
 * <p>Three accessors of {@link InvocationContext} return the elements of the interception as the reflection of the
 * platform describes them - {@code getMethod()} a {@code java.lang.reflect.Method}, {@code getConstructor()} a
 * {@code java.lang.reflect.Constructor} and {@code getInterceptorBindings()} instances of the binding annotations.
 * The specification leaves their types no way around, and resolving one is the only reflection an interception
 * does. Everything they describe, Micronaut already worked out at compilation time and holds without reflecting.</p>
 *
 * <p>Every context this module hands an interceptor method implements this interface, so an interceptor that would
 * rather read the compiled description asks for it:</p>
 *
 * <pre>{@code
 * @AroundInvoke
 * public Object invoke(InvocationContext context) throws Exception {
 *     if (context instanceof MicronautMethodInvocationContext micronaut) {
 *         ExecutableMethod<?, ?> method = micronaut.getExecutableMethod();
 *         log(method.getDeclaringType().getName() + "." + method.getMethodName());
 *     }
 *     return context.proceed();
 * }
 * }</pre>
 *
 * <p>An interceptor written against these accessors never reflects, and needs no reachability metadata of its own
 * in a GraalVM native image.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public interface MicronautInvocationContext extends InvocationContext {

    /**
     * The annotation metadata of the intercepted element, which Micronaut compiled.
     *
     * <p>This is what the interception itself resolves the chain by. It answers what
     * {@link InvocationContext#getInterceptorBindings()} answers, and the members of a binding besides, without
     * building an annotation: {@code getAnnotationMetadata().stringValue(Cached.class, "region")} rather than
     * finding the {@code @Cached} instance among the bindings and calling {@code region()} on it.</p>
     *
     * @return The metadata, never {@code null}
     */
    AnnotationMetadata getAnnotationMetadata();

    /**
     * Which of the interceptions of Micronaut this one is.
     *
     * <p>The specification lets one interceptor method interpose on more than one lifecycle event, and gives it no
     * way to tell which event it was invoked for. This does.</p>
     *
     * @return The kind
     */
    InterceptorKind getInterceptorKind();
}
