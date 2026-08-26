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

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;

import java.util.HashMap;
import java.util.Map;

/**
 * The interceptor instances of one intercepted object.
 *
 * <p>The specification associates an interceptor instance with the object it intercepts, so that an interceptor may
 * hold state for the whole life of that object. The advice that owns this map is itself created for every object it
 * intercepts, which is what makes the instances kept here belong to that one object and go away with it.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterceptorInstances {

    private final BeanContext beanContext;
    private final Map<Class<?>, Object> instances = new HashMap<>(4);

    InterceptorInstances(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * Returns the instance of an interceptor class, creating it the first time it is asked for.
     *
     * @param reference The interceptor
     * @return The instance
     */
    synchronized Object get(InterceptorReference reference) {
        // resolved by type rather than from one definition: an interceptor class may also be produced by a
        // factory, and the instance the application configured there is the one to intercept with
        return instances.computeIfAbsent(reference.interceptorClass(), beanContext::getBean);
    }
}
