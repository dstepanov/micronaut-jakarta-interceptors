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

import io.micronaut.aop.Intercepted;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.event.BeanDestroyedEvent;
import io.micronaut.context.event.BeanDestroyedEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.annotation.JakartaInterception;

import jakarta.inject.Singleton;

import java.util.Map;

/**
 * The lifecycle the specification gives an interceptor instance: it is created when the object it intercepts is
 * created, one set of instances per object, and destroyed when that object is destroyed.
 *
 * <p>The instances of each intercepted object are kept here, keyed by the object itself, so that the advice
 * bound to a method and the advice bound to the constructor reach the same instances. Creation is eager — as
 * the intercepted object is created, every interceptor instance of every chain of the object is created with
 * it — and destruction follows the object, after its own pre-destroy interception has run.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Singleton
@Context
@Internal
public final class InterceptorLifecycleSupport
    implements BeanCreatedEventListener<Object>, BeanDestroyedEventListener<Object> {

    private final Map<TargetKey, InterceptorInstances> instancesByTarget = new java.util.HashMap<>();
    private final java.lang.ref.ReferenceQueue<Object> collectedTargets = new java.lang.ref.ReferenceQueue<>();
    private final BeanContext beanContext;
    private volatile @org.jspecify.annotations.Nullable InterceptorChainResolver resolver;

    InterceptorLifecycleSupport(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    private InterceptorChainResolver resolver() {
        // fetched on first use rather than injected: a created-event listener is created before most beans,
        // and asking for the resolver while the listener itself is being created is a circle
        InterceptorChainResolver value = resolver;
        if (value == null) {
            value = beanContext.getBean(InterceptorChainResolver.class);
            resolver = value;
        }
        return value;
    }

    @Override
    public Object onCreated(BeanCreatedEvent<Object> event) {
        Object bean = event.getBean();
        if (!(bean instanceof Intercepted intercepted)) {
            return bean;
        }
        BeanDefinition<Object> definition = event.getBeanDefinition();
        InterceptorInstances instances = instancesFor(bean);
        for (InterceptorReference reference
            : resolver().chainOf(definition.getAnnotationMetadata(), io.micronaut.aop.InterceptorKind.POST_CONSTRUCT)) {
            createUnlessSelf(instances, reference);
        }
        for (InterceptorReference reference
            : resolver().chainOf(definition.getAnnotationMetadata(), io.micronaut.aop.InterceptorKind.PRE_DESTROY)) {
            createUnlessSelf(instances, reference);
        }
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            if (!method.getAnnotationMetadata().hasAnnotation(JakartaInterception.class)) {
                continue;
            }
            for (InterceptorReference reference
                : resolver().chainOf(method.getAnnotationMetadata(), io.micronaut.aop.InterceptorKind.AROUND)) {
                createUnlessSelf(instances, reference);
            }
        }
        return bean;
    }

    @Override
    public void onDestroyed(BeanDestroyedEvent<Object> event) {
        InterceptorInstances instances;
        synchronized (instancesByTarget) {
            purgeCollected();
            instances = instancesByTarget.remove(new TargetKey(event.getBean()));
        }
        if (instances != null) {
            // after the object's own pre-destroy interception has run: what is left is letting the interceptor
            // instances go, dependents and all
            instances.destroyAll();
        }
    }

    /**
     * The interceptor instances of the given intercepted object, created if the object has none yet.
     *
     * @param target The intercepted object
     * @return Its interceptor instances
     */
    InterceptorInstances instancesFor(Object target) {
        synchronized (instancesByTarget) {
            purgeCollected();
            InterceptorInstances instances = instancesByTarget.get(new TargetKey(target));
            if (instances == null) {
                instances = new InterceptorInstances(beanContext);
                instancesByTarget.put(new TargetKey(target, collectedTargets), instances);
            }
            return instances;
        }
    }

    /**
     * Forgets the interceptor instances of every object that has been collected. An intercepted object the
     * container never destroys — a prototype the application simply drops — would otherwise be held here for
     * as long as the container runs, and its interceptor instances with it. What is dropped this way is not
     * destroyed: an object nobody holds any more has had no pre-destroy of its own either.
     *
     * <p>Called with the monitor held.</p>
     */
    private void purgeCollected() {
        java.lang.ref.Reference<?> collected;
        while ((collected = collectedTargets.poll()) != null) {
            instancesByTarget.remove(collected);
        }
    }

    /**
     * Hands an intercepted object the instances that ran while it was being constructed, so that an interceptor
     * class interposing on both the construction and the methods of the object is one instance, not two.
     *
     * @param target  The constructed object
     * @param created The instances created for its construction
     */
    void adopt(Object target, InterceptorInstances created) {
        synchronized (instancesByTarget) {
            purgeCollected();
            InterceptorInstances existing = instancesByTarget.get(new TargetKey(target));
            if (existing == null) {
                instancesByTarget.put(new TargetKey(target, collectedTargets), created);
            } else {
                existing.adoptAll(created);
            }
        }
    }

    private void createUnlessSelf(InterceptorInstances instances, InterceptorReference reference) {
        if (!reference.self()) {
            instances.get(reference);
        }
    }

    /**
     * A key that holds the intercepted object weakly and compares by identity: interception belongs to the one
     * object, whatever its class makes of {@code equals}, and must not be what keeps it alive.
     */
    private static final class TargetKey extends java.lang.ref.WeakReference<Object> {

        private final int hash;

        /**
         * A key for looking one up, which is never stored and so needs no queue.
         */
        private TargetKey(Object target) {
            super(target);
            this.hash = System.identityHashCode(target);
        }

        private TargetKey(Object target, java.lang.ref.ReferenceQueue<Object> queue) {
            super(target, queue);
            this.hash = System.identityHashCode(target);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TargetKey key)) {
                return false;
            }
            Object target = get();
            return target != null && target == key.get();
        }
    }
}
