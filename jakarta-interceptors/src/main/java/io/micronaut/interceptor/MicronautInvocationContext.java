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
 * The interception as Micronaut compiled it, offered alongside what the specification describes.
 *
 * <p>Three accessors of {@link InvocationContext} describe the intercepted element with the reflection of the
 * platform, because the types the specification gives them leave no way around it: {@link #getMethod()} returns a
 * {@link java.lang.reflect.Method}, {@link #getConstructor()} a {@link java.lang.reflect.Constructor}, and
 * {@link #getInterceptorBindings()} instances of the binding annotations. Resolving one is the only reflection an
 * interception performs.</p>
 *
 * <p>It is not cheap reflection. Looking a member up inflates the declared members of the class into the
 * reflection data the virtual machine keeps for it, and building an annotation instance defines a proxy class that
 * lives as long as its class loader. Everything those three describe, Micronaut worked out while the application
 * was compiled and holds without reflecting on anything.</p>
 *
 * <p>Every context this module hands an interceptor method implements this interface, so an interceptor that would
 * rather read the compiled description asks for it and always gets it:</p>
 *
 * <pre>{@code
 * @Interceptor
 * @Cached(region = "users")
 * public class CachingInterceptor {
 *
 *     @AroundInvoke
 *     public Object invoke(InvocationContext context) throws Exception {
 *         if (context instanceof MicronautMethodInvocationContext micronaut) {
 *             ExecutableMethod<?, ?> method = micronaut.getExecutableMethod();
 *             String region = micronaut.getAnnotationMetadata()
 *                 .stringValue(Cached.class, "region")
 *                 .orElse("default");
 *             cache(region, method.getDeclaringType(), method.getMethodName());
 *         }
 *         return context.proceed();
 *     }
 * }
 * }</pre>
 *
 * <p>Which sub-interface a context implements follows what is being intercepted:
 * {@link MicronautMethodInvocationContext} for a business method, a timeout method or a lifecycle callback, and
 * {@link MicronautConstructorInvocationContext} for the construction of a bean. An interceptor that interposes on
 * one kind can name that sub-interface directly; one that interposes on several can test for this interface and
 * branch on {@link #getInterceptorKind()}.</p>
 *
 * <p>An interceptor written against these accessors reflects on nothing, whatever it reads, and needs no
 * reachability metadata of its own in a GraalVM native image. The accessors of the specification keep working
 * beside them, and an interceptor may use both.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public interface MicronautInvocationContext extends InvocationContext {

    /**
     * The annotation metadata of the intercepted element, as Micronaut compiled it.
     *
     * <p>This is what the interception itself resolved the chain by, and it is the reflection-free answer to
     * {@link #getInterceptorBindings()}: the bindings in effect are the annotations it carries that are
     * meta-annotated {@code @InterceptorBinding}, and the members of one are read from it directly.</p>
     *
     * <pre>{@code
     * // instead of finding the @Cached instance among getInterceptorBindings() and calling region() on it,
     * // which builds the annotation as a proxy
     * String region = context.getAnnotationMetadata().stringValue(Cached.class, "region").orElse("default");
     * }</pre>
     *
     * <p>For a method it is the metadata of the method over that of its class, so a binding a method declares is
     * seen ahead of the one it would otherwise inherit. For the construction of a bean it is the metadata of the
     * constructor, which the processor completes with the bindings of the class.</p>
     *
     * @return The metadata, never {@code null}
     */
    AnnotationMetadata getAnnotationMetadata();

    /**
     * Which of the interceptions of Micronaut this one is.
     *
     * <p>{@link InterceptorKind#AROUND} for a business or timeout method, {@link InterceptorKind#AROUND_CONSTRUCT}
     * for the construction of a bean, {@link InterceptorKind#POST_CONSTRUCT} and
     * {@link InterceptorKind#PRE_DESTROY} for the lifecycle callbacks.</p>
     *
     * <p>The specification lets one interceptor method interpose on more than one lifecycle event - a single method
     * annotated both {@code @PostConstruct} and {@code @PreDestroy} - and gives it no way to ask which event it was
     * invoked for. This is that way.</p>
     *
     * @return The kind, never {@code null}
     */
    InterceptorKind getInterceptorKind();
}
