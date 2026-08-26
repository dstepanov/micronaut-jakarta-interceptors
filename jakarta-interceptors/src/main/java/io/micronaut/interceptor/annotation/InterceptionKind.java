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
 * member of {@link JakartaInterceptorMethods} that records its interceptor method and, where there is one, the
 * Micronaut kind it is intercepted as.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public enum InterceptionKind {

    /**
     * A business method of the intercepted bean.
     */
    AROUND_INVOKE("aroundInvoke", InterceptorKind.AROUND),

    /**
     * A method the scheduler invokes, which is what the specification calls a timeout method.
     */
    AROUND_TIMEOUT("aroundTimeout", InterceptorKind.AROUND),

    /**
     * The construction of the intercepted bean.
     */
    AROUND_CONSTRUCT("aroundConstruct", InterceptorKind.AROUND_CONSTRUCT),

    /**
     * The post-construct callback of the intercepted bean.
     */
    POST_CONSTRUCT("postConstruct", InterceptorKind.POST_CONSTRUCT),

    /**
     * The pre-destroy callback of the intercepted bean.
     */
    PRE_DESTROY("preDestroy", InterceptorKind.PRE_DESTROY);

    private final String member;
    private final InterceptorKind interceptorKind;

    InterceptionKind(String member, InterceptorKind interceptorKind) {
        this.member = member;
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
     * @param interceptorKind The Micronaut kind
     * @param timeout         Whether the intercepted method is one the scheduler invokes
     * @return The kind, or {@code null} when Micronaut intercepts something the specification does not describe
     */
    public static @Nullable InterceptionKind of(InterceptorKind interceptorKind, boolean timeout) {
        return switch (interceptorKind) {
            case AROUND -> timeout ? AROUND_TIMEOUT : AROUND_INVOKE;
            case AROUND_CONSTRUCT -> AROUND_CONSTRUCT;
            case POST_CONSTRUCT -> POST_CONSTRUCT;
            case PRE_DESTROY -> PRE_DESTROY;
            default -> null;
        };
    }
}
