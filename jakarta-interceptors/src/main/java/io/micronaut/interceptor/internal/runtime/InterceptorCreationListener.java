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

import io.micronaut.context.BeanRegistration;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates the interceptor instances of an object as the object is created.
 *
 * <p>Section 2.3 has an interceptor instance created for each interceptor class when the target instance is
 * created. The interception itself has no reason to create one before it needs it, so this asks for them: what an
 * interceptor class does as it is constructed happens when the object it interposes on is constructed, even where
 * nothing is ever invoked on that object.</p>
 *
 * <p>The instances belong to the advice Micronaut created for the object, which is among the dependents of the
 * object. Creating them through that advice is what makes them the same instances the interceptions of the object go
 * on to use, rather than a second set nobody reads.</p>
 *
 * <p>It is only needed for a bean created through {@code BeanContext.createBean}. Micronaut hands the advice of a
 * bean it creates for a lookup or an injection the bean it is created for - the subclass it generates to intercept
 * the methods of a class, the target of a proxy, and the bean whose lifecycle is intercepted alike - and the advice
 * creates the instances itself, see {@link JakartaInterceptorAdvice}; such an advice is asked for nothing here. The
 * advice of a bean created through {@code createBean} is handed none, because Micronaut creates that bean without
 * the creation its lookups begin, which is what the bean is known to the interceptors by.</p>
 *
 * <p>Micronaut offers no listener narrowed to the beans an annotation is declared on, so this one is handed every
 * bean the context creates. It returns at once for an object whose definition declares nothing this module
 * intercepts, before looking at any of its dependents. Whether a definition declares anything is read once for it,
 * rather than from each of its executable methods every time a bean of it is created: a definition Micronaut
 * compiled is equal to every other instance of its class, so the answer is remembered once for each. See the guide,
 * "Waiting on Micronaut core".</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Singleton
@Internal
final class InterceptorCreationListener implements BeanCreatedEventListener<Object> {

    private final Map<BeanDefinition<?>, Boolean> intercepted = new ConcurrentHashMap<>();

    @Override
    public Object onCreated(BeanCreatedEvent<Object> event) {
        if (!intercepted.computeIfAbsent(event.getBeanDefinition(), JakartaInterceptorAdvice::intercepts)) {
            // nothing of the object is intercepted by this module, so it has no interceptor instance to create
            return event.getBean();
        }
        for (BeanRegistration<?> registration : event.getDependentBeans()) {
            if (registration.getBean() instanceof JakartaInterceptorAdvice advice) {
                advice.createInterceptorInstances(event.getBeanDefinition());
            }
        }
        return event.getBean();
    }
}
