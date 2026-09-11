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

import io.micronaut.aop.InterceptorKind;
import org.jspecify.annotations.Nullable;

/**
 * The kinds of interception the Jakarta Interceptors specification defines.
 *
 * <p>They are not quite the kinds Micronaut knows: the specification also interposes on the timeout methods a timer
 * service invokes, which Micronaut intercepts as it does any other method. Each kind therefore names both the
 * members of {@link JakartaInterceptorMethods} that record its interceptor methods and, where there is one, the
 * Micronaut kind it is intercepted as.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public enum InterceptionKind {

    /**
     * A business method of the intercepted bean.
     */
    AROUND_INVOKE("aroundInvoke", "aroundInvokeDeclaringTypes", InterceptorKind.AROUND),

    /**
     * A method the scheduler invokes, which is what the specification calls a timeout method.
     */
    AROUND_TIMEOUT("aroundTimeout", "aroundTimeoutDeclaringTypes", InterceptorKind.AROUND),

    /**
     * The construction of the intercepted bean.
     */
    AROUND_CONSTRUCT("aroundConstruct", "aroundConstructDeclaringTypes", InterceptorKind.AROUND_CONSTRUCT),

    /**
     * The post-construct callback of the intercepted bean.
     */
    POST_CONSTRUCT("postConstruct", "postConstructDeclaringTypes", InterceptorKind.POST_CONSTRUCT),

    /**
     * The pre-destroy callback of the intercepted bean.
     */
    PRE_DESTROY("preDestroy", "preDestroyDeclaringTypes", InterceptorKind.PRE_DESTROY);

    private final String member;
    private final String declaringTypesMember;
    private final InterceptorKind interceptorKind;

    InterceptionKind(String member, String declaringTypesMember, InterceptorKind interceptorKind) {
        this.member = member;
        this.declaringTypesMember = declaringTypesMember;
        this.interceptorKind = interceptorKind;
    }

    /**
     * The member of {@link JakartaInterceptorMethods} that records the interceptor method of this kind.
     *
     * @return The member name
     */
    public String member() {
        return member;
    }

    /**
     * The member of {@link JakartaInterceptorMethods} that records the classes declaring the interceptor methods
     * {@link #member()} names, in the same order.
     *
     * @return The member name
     */
    public String declaringTypesMember() {
        return declaringTypesMember;
    }

    /**
     * The Micronaut kind of interception this one is carried by.
     *
     * @return The Micronaut kind
     */
    public InterceptorKind interceptorKind() {
        return interceptorKind;
    }

    /**
     * The kind that interposes on an invocation Micronaut intercepts as the given kind.
     *
     * <p>Micronaut reports the invocation of a method of a bean it implements with introduction advice as an
     * introduction rather than as around advice, whether the advice implements that method or the method is a
     * concrete one of the bean. The specification knows no such distinction: either is the invocation of a business
     * method, or of a timeout method where the scheduler invokes it, and is interposed on as one.</p>
     *
     * @param interceptorKind The Micronaut kind
     * @param timeout         Whether the intercepted method is one the scheduler invokes
     * @return The kind, or {@code null} when Micronaut intercepts something the specification does not describe
     */
    public static @Nullable InterceptionKind of(InterceptorKind interceptorKind, boolean timeout) {
        return switch (interceptorKind) {
            case AROUND, INTRODUCTION -> timeout ? AROUND_TIMEOUT : AROUND_INVOKE;
            case AROUND_CONSTRUCT -> AROUND_CONSTRUCT;
            case POST_CONSTRUCT -> POST_CONSTRUCT;
            case PRE_DESTROY -> PRE_DESTROY;
            default -> null;
        };
    }
}
