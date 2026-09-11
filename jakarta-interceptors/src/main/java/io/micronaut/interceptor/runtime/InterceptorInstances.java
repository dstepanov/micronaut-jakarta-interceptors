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
import io.micronaut.context.BeanResolutionContext;
import io.micronaut.context.DefaultBeanContext;
import io.micronaut.context.DefaultBeanResolutionContext;
import io.micronaut.core.annotation.Internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The interceptor instances of one intercepted object.
 *
 * <p>The specification associates an interceptor instance with the object it intercepts, so that an interceptor may
 * hold state for the whole life of that object. The advice that owns this map is itself created for every object it
 * intercepts, which is what makes the instances kept here belong to that one object and go away with it.</p>
 *
 * <p>Going away includes being destroyed. Section 2.3 destroys the interceptor instances of an object when the object
 * is removed or fails to be created, so that an interceptor's own {@code @PreDestroy} runs and what it was injected
 * with goes too. Micronaut destroys what was created for an object alone together with that object, and this is what
 * tells it which instances those are: each is created the way Micronaut creates a dependency of a bean, and the
 * registrations Micronaut then reports as dependents are kept, to be destroyed with the object.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterceptorInstances {

    private final BeanContext beanContext;
    private final Map<Class<?>, Object> instances = new HashMap<>(4);
    /**
     * The registrations of the instances that belong to this object alone, in the order they were created. An
     * interceptor with a scope of its own - a {@code @Singleton} shared by every object it intercepts - is not
     * among them: it is not the object's to destroy.
     */
    private final List<BeanRegistration<?>> owned = new ArrayList<>(2);
    /**
     * Whether the post-construct event of the object was intercepted, which makes its pre-destroy event, rather than
     * the destruction of the advice, what ends these instances.
     */
    private volatile boolean lifecycleIntercepted;

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
        Class<?> interceptorClass = reference.interceptorClass();
        Object instance = instances.get(interceptorClass);
        if (instance == null) {
            instance = create(interceptorClass);
            instances.put(interceptorClass, instance);
        }
        return instance;
    }

    /**
     * Creates the instance of every interceptor class of a chain that has none yet.
     *
     * @param chain The chain
     */
    void createAll(List<InterceptorReference> chain) {
        for (InterceptorReference reference : chain) {
            // an interceptor method the intercepted class declares itself runs on the object, and has no instance
            // of its own to create
            if (!reference.self()) {
                get(reference);
            }
        }
    }

    /**
     * Creates the instance of an interceptor class.
     *
     * <p>Resolved by type rather than from one definition: an interceptor class may also be produced by a factory,
     * and the instance the application configured there is the one to intercept with.</p>
     *
     * <p>Resolved in a resolution context of its own, which is where Micronaut records what it created as a
     * dependency rather than found in a scope. An instance of a class without a scope, or of a
     * {@code @Prototype}, is recorded there, together with what it was injected with; a singleton, or an instance
     * of a custom scope, is not. Micronaut decides that exactly as it does for the dependencies of any bean, so
     * nothing here has to know the scopes.</p>
     */
    private Object create(Class<?> interceptorClass) {
        if (!(beanContext instanceof DefaultBeanContext)) {
            // a context of another implementation records no dependents to read
            return beanContext.getBean(interceptorClass);
        }
        try (BeanResolutionContext resolutionContext = new DefaultBeanResolutionContext(beanContext, null)) {
            Object instance = resolutionContext.getBean(interceptorClass);
            owned.addAll(resolutionContext.getAndResetDependentBeans());
            return instance;
        }
    }

    /**
     * Records that the post-construct event of the object is being intercepted with these instances.
     */
    void postConstructed() {
        lifecycleIntercepted = true;
    }

    /**
     * Destroys these instances once the pre-destroy event of the object has been intercepted, which is where section
     * 2.3 destroys them: after the pre-destroy interceptor methods, and with the object.
     */
    void preDestroyed() {
        destroy();
    }

    /**
     * Destroys these instances as the advice holding them is destroyed, unless the pre-destroy event of the object
     * will.
     *
     * <p>Micronaut destroys the advice as a dependent of the object, after the pre-destroy event of the object, so
     * for an object whose lifecycle is intercepted the instances are gone by then and this does nothing. It may also
     * destroy the advice long before: a bean a {@code @Factory} method produces, whose interceptors are resolved
     * before the factory is, has the first of them taken for the factory and destroyed as soon as the bean is
     * created. The object is alive, and its pre-destroy event is intercepted with this same advice later, so for an
     * object whose post-construct event was intercepted it is that event that destroys the instances. An object
     * whose lifecycle is not intercepted - one bound only on its methods - has no such event, and its instances go
     * with the advice.</p>
     */
    void adviceDestroyed() {
        if (!lifecycleIntercepted) {
            destroy();
        }
    }

    /**
     * Destroys the interceptor instances that belong to this object alone, latest first, as Micronaut destroys the
     * dependents of a bean. Destroying them twice destroys them once.
     *
     * <p>The instances are forgotten as well, so that nothing goes on to intercept with an instance that has been
     * destroyed.</p>
     */
    void destroy() {
        List<BeanRegistration<?>> destroyed;
        synchronized (this) {
            if (owned.isEmpty()) {
                instances.clear();
                return;
            }
            destroyed = new ArrayList<>(owned);
            owned.clear();
            instances.clear();
        }
        // outside of the lock: what an interceptor does as it is destroyed is its own code
        for (int i = destroyed.size() - 1; i >= 0; i--) {
            beanContext.destroyBean(destroyed.get(i));
        }
    }
}
