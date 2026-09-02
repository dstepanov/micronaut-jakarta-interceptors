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
package io.micronaut.interceptor.annotation;

import io.micronaut.aop.InterceptorBinding;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.core.annotation.Internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The interceptor binding the processor declares on every element the Jakarta Interceptors specification
 * intercepts, and which carries the interceptor classes that element names directly.
 *
 * <p>It is the single binding annotation of the module: one interceptor bean, the advice, is bound by it, and the
 * advice runs the interceptor classes that apply to the element. The interceptor classes an element names with
 * {@code jakarta.interceptor.Interceptors} are resolved at compilation time and listed here in the order the
 * specification invokes them in; the interceptor classes bound by a binding annotation are resolved from the bean
 * context, since they may well be compiled separately from the element they intercept.</p>
 *
 * <p>It declares a binding of every kind of interception, so that an element carrying it is intercepted whichever
 * kind of interceptor method applies to it; a kind no interceptor class implements resolves to an empty chain and
 * proceeds straight away. That it declares a binding of the {@link InterceptorKind#AROUND} kind is also what makes
 * Micronaut generate the proxy for the element.</p>
 *
 * <p>The members take no part in the binding, which is the default of a Micronaut interceptor binding, so an
 * element with its own list of interceptor classes still binds to the same advice.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@InterceptorBinding(kind = InterceptorKind.AROUND)
@InterceptorBinding(kind = InterceptorKind.AROUND_CONSTRUCT)
@InterceptorBinding(kind = InterceptorKind.POST_CONSTRUCT)
@InterceptorBinding(kind = InterceptorKind.PRE_DESTROY)
@Internal
public @interface JakartaInterception {

    /**
     * The interceptor classes the intercepted element names with {@code jakarta.interceptor.Interceptors}, in the
     * order the specification invokes them in: the ones the class names first, then the ones the member names.
     *
     * @return The interceptor classes
     */
    Class<?>[] interceptors() default {};

    /**
     * The class that declares interceptor methods on itself, which the specification invokes after every
     * interceptor class. Its interceptor method is invoked on the intercepted instance rather than on an
     * interceptor of its own.
     *
     * @return The class, or {@code void} when the intercepted class declares no interceptor method
     */
    Class<?> self() default void.class;

    /**
     * Tells that the element is not intercepted at all, which shadows what the class it belongs to declares. An
     * interceptor method a class declares on itself is such an element: it is not a business method.
     *
     * @return Whether the element is excluded from interception
     */
    boolean excluded() default false;

    /**
     * Tells that the element is a method the scheduler invokes, which is what the specification calls a timeout
     * method and interposes on with {@code jakarta.interceptor.AroundTimeout} rather than with
     * {@code jakarta.interceptor.AroundInvoke}.
     *
     * @return Whether the element is a timeout method
     */
    boolean timeout() default false;

    /**
     * The name of the post-construct callback of the intercepted class itself, which the specification hands to a
     * {@code @PostConstruct} interceptor method as the method it is interposing on.
     *
     * <p>It is the callback of the class rather than the one of an interceptor class, and it is the most specific
     * one when a class and its superclasses each declare one. The interception happens once around all of them,
     * so it is that one the interceptor is shown.</p>
     *
     * @return The name of the callback, or the empty string when the class declares none
     */
    String postConstruct() default "";

    /**
     * The name of the pre-destroy callback of the intercepted class itself.
     *
     * @return The name of the callback, or the empty string when the class declares none
     * @see #postConstruct()
     */
    String preDestroy() default "";

    /**
     * The binding annotations in effect on the element, each written out as one string by the processor.
     *
     * <p>An interceptor class is bound to the element when every binding it declares is one of these. Comparing
     * the two is comparing strings: what a binding is compared by - the members it declares, the ones it defaults
     * to, less the ones excluded from the binding - was worked out at compilation time.</p>
     *
     * @return The bindings of the element
     */
    String[] bindings() default {};
}
