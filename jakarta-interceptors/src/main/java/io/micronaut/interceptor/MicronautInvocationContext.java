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
import io.micronaut.core.annotation.AnnotationValue;
import jakarta.interceptor.InvocationContext;

import java.util.List;

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
 *             ExecutableMethod<Object, Object> method = micronaut.getMicronautInvocation().getExecutableMethod();
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
 * branch on the {@link io.micronaut.aop.InvocationContext#getKind() kind} of {@link #getMicronautInvocation()}.</p>
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
     * <p>It is ordinary Micronaut metadata, read with the semantics Micronaut gives it everywhere, and every
     * annotation of the element is read from it without reflecting - the bindings among them, and the annotations
     * that bind nothing.</p>
     *
     * <pre>{@code
     * String region = context.getAnnotationMetadata().stringValue(Cached.class, "region").orElse("default");
     * }</pre>
     *
     * <p>For a method it is the metadata of the method over that of its class, and Micronaut answers a question
     * about an annotation from the two together. That is not always what selected the interceptors: a binding a
     * method declares replaces the one of the same type its class declares as a whole, while the metadata merges a
     * member the method leaves to its default with the one the class gave it, and answers a repeatable binding with
     * the occurrences of both. On a class declaring {@code @Cached(region = "users")}, a method declaring
     * {@code @Cached} is bound by the default region, and the metadata says {@code "users"}.
     * {@link #getInterceptorBindingValues()} reads the bindings as they selected the interceptors.</p>
     *
     * <p>For the construction of a bean it is the metadata of the constructor, which the processor completes with
     * the bindings of the class. That is why it is offered here rather than left to {@link #getMicronautInvocation()}:
     * a Micronaut construction carries no metadata of its own, its constructor does.</p>
     *
     * @return The metadata, never {@code null}
     */
    AnnotationMetadata getAnnotationMetadata();

    /**
     * The interceptor bindings in effect on the intercepted element, as the values of their binding annotations, read
     * without reflecting and without building an annotation.
     *
     * <p>This is the reflection-free answer to {@link #getInterceptorBindings()}, and holds the same bindings: each
     * binding annotation the element carries, read from the nearest declaration of it - the method's where it
     * declares one, its class's otherwise - with a repeatable binding as the occurrences of that declaration. A member
     * a declaration leaves to its default is not filled in from the declaration of the class; it is left out, and
     * {@link AnnotationValue#getDefaultValues()} holds its default. The members that do not bind are reported as
     * declared, since an interceptor may read them, and an evaluated expression is reported as the expression, as
     * the comparison that bound the interceptor read it.</p>
     *
     * <pre>{@code
     * for (AnnotationValue<?> binding : context.getInterceptorBindingValues()) {
     *     if (binding.getAnnotationName().equals(Cached.class.getName())) {
     *         String region = binding.stringValue("region").orElse("default");
     *     }
     * }
     * }</pre>
     *
     * @return The bindings, in the order the metadata lists their annotations, empty where the element has none
     */
    List<AnnotationValue<?>> getInterceptorBindingValues();

    /**
     * The Micronaut invocation this interception is part of.
     *
     * <p>It describes the intercepted element as Micronaut compiled it, and reaches what the specification has no
     * accessor for. Its {@link io.micronaut.aop.InvocationContext#getKind() kind} tells which interception this is:
     * {@link InterceptorKind#AROUND} for a business or timeout method ({@link InterceptorKind#INTRODUCTION} for
     * one of a bean Micronaut implements with introduction advice), {@link InterceptorKind#AROUND_CONSTRUCT} for
     * the construction of a bean, {@link InterceptorKind#POST_CONSTRUCT} and {@link InterceptorKind#PRE_DESTROY}
     * for the lifecycle callbacks. The specification lets one interceptor method interpose on more than one
     * lifecycle event - a single method annotated both {@code @PostConstruct} and {@code @PreDestroy} - and gives
     * it no way to ask which event it was invoked for. The kind is that way.</p>
     *
     * <pre>{@code
     * switch (micronaut.getMicronautInvocation().getKind()) {
     *     case POST_CONSTRUCT -> opened(context.getTarget());
     *     case PRE_DESTROY -> closed(context.getTarget());
     *     default -> { }
     * }
     * }</pre>
     *
     * <p>{@link MicronautMethodInvocationContext} and {@link MicronautConstructorInvocationContext} narrow it to
     * the {@link io.micronaut.aop.MethodInvocationContext} or the {@link io.micronaut.aop.ConstructorInvocationContext}
     * it is.</p>
     *
     * <p>Do not call {@link io.micronaut.aop.InvocationContext#proceed()} on it. That is what the last interceptor
     * method of the chain proceeds into, so proceeding it directly skips every interceptor method the specification
     * has yet to invoke. Call {@link #proceed()} on this context instead, which is the chain the specification
     * describes.</p>
     *
     * @return The invocation, never {@code null}
     */
    io.micronaut.aop.InvocationContext<Object, Object> getMicronautInvocation();
}
