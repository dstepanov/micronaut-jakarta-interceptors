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
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.aop.InterceptorKind;
import io.micronaut.core.annotation.Internal;

import java.util.List;

/**
 * The chains of a method, kept by the method itself through
 * {@link io.micronaut.inject.ExecutableMethod#getCachedUnevaluatedValue}.
 *
 * <p>Micronaut documents the methods of a bean definition, and of the proxies a context creates, as belonging to
 * that context: what they keep is dropped with it and not seen by another context. The chains depend on the
 * interceptor classes of the context, and so belong on the method, which every proxy of the context shares. They
 * are built from the interception the processor recorded on the method, which holds no evaluated expression, so a
 * method another annotation of which holds one keeps them as well.</p>
 *
 * @param business  The chain of a business method invocation of the method
 * @param scheduled The chain of an invocation the scheduler makes, which is the business chain unless the method is
 *                  a timeout method
 * @param timeout   Whether the method is a timeout method when the scheduler invokes it
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record MethodChains(List<InterceptorReference> business,
                    List<InterceptorReference> scheduled,
                    boolean timeout) {

    /**
     * The chains of a lifecycle event of a bean, kept by the method Micronaut runs the event through.
     *
     * @param postConstruct The chain of the post-construct event
     * @param preDestroy    The chain of the pre-destroy event
     */
    @Internal
    record Lifecycle(List<InterceptorReference> postConstruct, List<InterceptorReference> preDestroy) {

        /**
         * The chain of one lifecycle event.
         *
         * @param kind The kind of the event
         * @return Its chain, none for a kind that is not a lifecycle event
         */
        List<InterceptorReference> of(InterceptorKind kind) {
            return switch (kind) {
                case POST_CONSTRUCT -> postConstruct;
                case PRE_DESTROY -> preDestroy;
                default -> List.of();
            };
        }
    }
}
