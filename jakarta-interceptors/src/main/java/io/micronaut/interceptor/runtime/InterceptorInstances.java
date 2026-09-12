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
import io.micronaut.context.DependentBeanProvider;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import org.jspecify.annotations.Nullable;

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
 * <p>A bean proxied with a separate target is one object to the specification and two to Micronaut: the proxy, whose
 * advice interposes on the business methods, and the target, whose own advice interposes on its construction and
 * lifecycle. The proxy then {@linkplain #share shares} the instances of the target, so that one interceptor instance
 * serves both, and they stay the target's, destroyed by the pre-destroy event of the target.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterceptorInstances {

    /**
     * The object whose post-construct event this thread intercepted last, until a proxy of it takes its instances or
     * it turns out to have no proxy.
     */
    private static final ThreadLocal<@Nullable PostConstructed> POST_CONSTRUCTED = new ThreadLocal<>();

    /**
     * The annotation that makes a bean of a custom scope resolve to a proxy over a target the scope keeps. Named
     * rather than referenced: it is declared by a module this one does not depend on, and what matters here is only
     * what Micronaut itself reads it for, which is the same name.
     */
    private static final String SCOPED_PROXY = "io.micronaut.runtime.context.scope.ScopedProxy";

    private final BeanContext beanContext;
    private final Map<Class<?>, Object> instances = new HashMap<>(4);
    /**
     * The registrations of the instances that belong to this object alone, in the order they were created. An
     * interceptor with a scope of its own - a {@code @Singleton}, or a bean of a custom scope, shared by every object
     * it intercepts - is not among them: it is not the object's to destroy. See {@link #own}.
     */
    private final List<BeanRegistration<?>> owned = new ArrayList<>(2);
    /**
     * Whether the post-construct event of the object was intercepted, which makes its pre-destroy event, rather than
     * the destruction of the advice, what ends these instances.
     */
    private volatile boolean lifecycleIntercepted;
    /**
     * The instances of the target this proxy was created for, which are the ones to intercept with, or {@code null}
     * where these are the instances of the object itself.
     */
    private volatile @Nullable InterceptorInstances shared;

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
        InterceptorInstances target = shared;
        if (target != null) {
            return target.get(reference);
        }
        synchronized (this) {
            Class<?> interceptorClass = reference.interceptorClass();
            Object instance = instances.get(interceptorClass);
            if (instance == null) {
                instance = create(interceptorClass);
                instances.put(interceptorClass, instance);
            }
            return instance;
        }
    }

    /**
     * Makes these the instances of a proxy whose target has the given instances, so that every interception of the
     * proxy uses the instances of the target.
     *
     * <p>Called as the proxy is created, before it has intercepted anything. The instances of the target stay the
     * target's: the advice of the proxy destroys none of them as it is destroyed, and the pre-destroy event of the
     * target does.</p>
     *
     * @param target The instances of the target
     */
    @SuppressWarnings("ReferenceEquality")
    void share(InterceptorInstances target) {
        // the instances of a proxy are never those of its own target; this only keeps a lookup from going round
        if (target != this) {
            shared = target;
        }
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
            for (BeanRegistration<?> registration : resolutionContext.getAndResetDependentBeans()) {
                own(registration);
            }
            return instance;
        }
    }

    /**
     * Keeps what a registration Micronaut reported as a dependent leaves this object to destroy.
     *
     * <p>A dependent is usually the object's alone, and is kept as it is. A bean of a custom scope that resolves
     * through a proxy is not: Micronaut reports the proxy as a dependent, while the bean behind it belongs to the
     * scope and serves every object the interceptor is bound to. Destroying such a registration in its own right
     * removes that bean from its scope, which is how destroying one intercepted object used to destroy an interceptor
     * another was still intercepted by. Micronaut does not do that when it destroys a proxy as the dependent of a
     * bean - it destroys what the proxy itself depends on and leaves the scope alone - and the dependents of the
     * proxy are what is kept here instead, so that the resources the interceptor really holds of its own still go.</p>
     */
    private void own(BeanRegistration<?> registration) {
        BeanDefinition<?> definition = registration.getBeanDefinition();
        if (definition.isProxy() && definition.hasStereotype(SCOPED_PROXY)) {
            if (registration instanceof DependentBeanProvider provider) {
                for (BeanRegistration<?> dependent : provider.dependentBeans()) {
                    own(dependent);
                }
            }
            return;
        }
        owned.add(registration);
    }

    /**
     * Records that the post-construct event of an object has been intercepted with these instances.
     *
     * <p>The object is also handed to the proxy Micronaut may be creating around it, as its target: see
     * {@link #takePostConstructed}.</p>
     *
     * @param target The object
     */
    void postConstructed(Object target) {
        lifecycleIntercepted = true;
        POST_CONSTRUCTED.set(new PostConstructed(target, this));
    }

    /**
     * Destroys the instances in use once the pre-destroy event of the object has been intercepted, which is where
     * section 2.3 destroys them: after the pre-destroy interceptor methods, and with the object.
     *
     * <p>The instances in use are those of the target, for a proxy that shares them. Micronaut destroys a proxy and
     * its target together, and where it has lost track of the advice of the target it intercepts the pre-destroy
     * event of the target with the advice of the proxy.</p>
     */
    void preDestroyed() {
        InterceptorInstances target = shared;
        (target == null ? this : target).destroy();
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
     * with the advice. The advice of a proxy that shares the instances of its target has none of its own to
     * destroy.</p>
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

    /**
     * Returns the instances the post-construct event of the given object was intercepted with, when it is the last
     * such event this thread intercepted, and forgets them.
     *
     * <p>A proxy with a separate target resolves its target as the proxy is constructed, so on the thread creating
     * the proxy the post-construct event of the target is intercepted just before the proxy is complete. Reading it
     * back once the proxy is complete is how the proxy finds the instances of its target. Nothing else links the
     * two: the advice of the target is a dependent of the target, and Micronaut does not expose the dependents of a
     * bean to a proxy being created around it; for a bean a factory produced it does not even keep that advice as
     * one, taking it for the factory instead. Holding one object per thread, and only until the next bean is
     * created, links them without a map that would outlive them.</p>
     *
     * @param target The target of the proxy being created
     * @return The instances of that target, or {@code null} when its post-construct event was not the last one this
     * thread intercepted
     */
    static @Nullable InterceptorInstances takePostConstructed(Object target) {
        PostConstructed last = POST_CONSTRUCTED.get();
        if (last == null || !last.is(target)) {
            return null;
        }
        POST_CONSTRUCTED.remove();
        return last.instances();
    }

    /**
     * Forgets the object whose post-construct event this thread intercepted last, unless it is the given target of a
     * proxy that is still to be completed.
     *
     * <p>Called as each bean is created. The target of a proxy is created as the proxy is constructed, and the proxy
     * is complete right after it, so any other bean completed in between means the proxy will not come for the
     * instances; nothing is held for longer than that.</p>
     *
     * @param proxyTarget The bean just created, when it is the target of a proxy, otherwise {@code null}
     */
    static void forgetPostConstructedExcept(@Nullable Object proxyTarget) {
        PostConstructed last = POST_CONSTRUCTED.get();
        if (last != null && (proxyTarget == null || !last.is(proxyTarget))) {
            POST_CONSTRUCTED.remove();
        }
    }

    /**
     * An object whose post-construct event was intercepted, and the instances it was intercepted with.
     *
     * @param target    The object
     * @param instances The instances
     */
    private record PostConstructed(Object target, InterceptorInstances instances) {

        /**
         * Whether this is the given object itself, rather than one equal to it.
         */
        @SuppressWarnings("ReferenceEquality")
        boolean is(Object bean) {
            return target == bean;
        }
    }
}
