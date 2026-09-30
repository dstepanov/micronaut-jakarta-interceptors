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
import io.micronaut.inject.BeanDefinition;

/**
 * Decides whether an interceptor class bound by an interceptor binding takes part in interception.
 *
 * <p>Jakarta Interceptors leaves the enablement of such an interceptor to whatever uses it, and on its own this
 * module enables every interceptor class of the context, ordering one that declares no priority at
 * {@code Interceptor.Priority.APPLICATION}. A module with rules of its own - Contexts and Dependency Injection,
 * which enables an interceptor by the priority it declares - provides a bean of this type, and the chains this
 * runtime resolves then leave out an interceptor class it does not enable. Deciding it here, where the chain is
 * resolved, is what keeps the interceptors that run the ones such a module reports.</p>
 *
 * <p>Only the interceptor classes a binding annotation binds are asked about. An interceptor class an element names
 * with {@code jakarta.interceptor.Interceptors} is enabled by being named, as is the interceptor method a class
 * declares on itself. A chain is remembered once it is resolved, so the answer for an interceptor class must not
 * change while the context runs.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public interface BoundInterceptorEnablement {

    /**
     * Whether an interceptor class bound by an interceptor binding is enabled.
     *
     * @param interceptor The definition that describes the interceptor class
     * @return Whether the interceptor class takes part in the chains its bindings bind it to
     */
    boolean isEnabled(BeanDefinition<?> interceptor);

    /**
     * The position of an interceptor class among the ones that are ordered by being listed rather than by a
     * priority, where the module has such a list.
     *
     * <p>The interceptor classes a binding binds are ordered by their priority, and on its own this module orders
     * every one of them so. An interceptor class with a position comes after all of those, in the order of the
     * positions: the way Contexts and Dependency Injection orders the interceptors enabled by being listed for a
     * bean archive after the ones enabled by a priority.</p>
     *
     * @param interceptor The definition that describes the interceptor class
     * @return The position, or a negative number for an interceptor class ordered by its priority
     */
    default int position(BeanDefinition<?> interceptor) {
        return -1;
    }
}
