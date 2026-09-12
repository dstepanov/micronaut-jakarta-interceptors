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
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The interceptor instances of one intercepted object.
 *
 * <p>The specification associates an interceptor instance with the object it intercepts, so that an interceptor may
 * hold state for the whole life of that object, and destroys the instance when the object is destroyed. The advice
 * that owns this map is itself created for every object it intercepts and destroyed with it, which is what makes
 * the instances kept here belong to that one object and go away with it.</p>
 *
 * <p>An instance is held as the registration the bean context created it under, which is what destroys it and
 * whatever was injected into it. Only an instance created for this object is destroyed with it: a singleton
 * interceptor is shared by every object it intercepts, and an interceptor in a scope of its own lives as long as
 * that scope does.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterceptorInstances {

    private final BeanContext beanContext;
    private final Map<Class<?>, BeanRegistration<?>> instances = new ConcurrentHashMap<>(4);

    InterceptorInstances(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * Returns the instance of an interceptor class, creating it the first time it is asked for.
     *
     * @param reference The interceptor
     * @return The instance
     */
    Object get(InterceptorReference reference) {
        Class<?> interceptorClass = reference.interceptorClass();
        BeanRegistration<?> registration = instances.get(interceptorClass);
        if (registration == null) {
            // created outside the map rather than inside a computeIfAbsent, which would hold a lock of the map
            // across the bean context while the interceptor and whatever it injects are created. Two threads may
            // create one each; the first to put it in wins, and the other is destroyed again.
            //
            // resolved by type rather than from one definition: an interceptor class may also be produced by a
            // factory, and the instance the application configured there is the one to intercept with
            BeanRegistration<?> created = beanContext.getBeanRegistration(interceptorClass, null);
            registration = instances.putIfAbsent(interceptorClass, created);
            if (registration == null) {
                registration = created;
            } else {
                destroy(created);
            }
        }
        return registration.bean();
    }

    /**
     * Destroys every instance created for the intercepted object, once the object itself has been destroyed.
     */
    void destroyAll() {
        for (BeanRegistration<?> registration : instances.values()) {
            destroy(registration);
        }
        instances.clear();
    }

    private static void destroy(BeanRegistration<?> registration) {
        if (createdForOneObject(registration.getBeanDefinition())) {
            registration.close();
        }
    }

    /**
     * Whether an instance of the definition was created for the object it intercepts, and so is destroyed with
     * it: one that is neither a singleton, which every intercepted object shares, nor in a scope of its own,
     * which decides its life for itself.
     */
    private static boolean createdForOneObject(BeanDefinition<?> definition) {
        if (definition.isSingleton()) {
            return false;
        }
        Optional<String> scope = definition.getScopeName();
        return scope.isEmpty() || scope.get().equals(Prototype.class.getName());
    }
}
