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

import io.micronaut.aop.HotSwappableInterceptedProxy;
import io.micronaut.aop.Intercepted;
import io.micronaut.aop.InterceptedProxy;
import io.micronaut.aop.Interceptor;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanDefinitionReference;
import io.micronaut.inject.DelegatingBeanDefinition;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Creates the interceptor instances of an object as the object is created.
 *
 * <p>Section 2.3 has an interceptor instance created for each interceptor class when the target instance is
 * created. The interception itself has no reason to create one before it needs it, so this asks for them: what an
 * interceptor class does as it is constructed happens when the object it interposes on is constructed, even where
 * nothing is ever invoked on that object.</p>
 *
 * <p>The instances belong to the advice Micronaut bound to the object, which the generated proxy carries and hands
 * over here. Creating them through that advice is what makes them the same instances the interceptions of the
 * object go on to use, rather than a second set nobody reads.</p>
 *
 * <p>A proxy with a separate target - {@code @Around(proxyTarget = true)}, or any bean a factory produces - carries
 * advice of its own, and the target was created, and had its construction and lifecycle intercepted, with another.
 * The advice of the proxy is made to use the interceptor instances of the target before it creates any, so that one
 * instance of an interceptor class serves the whole of the one object the specification sees.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Singleton
@Internal
final class InterceptorCreationListener implements BeanCreatedEventListener<Object> {

    @Override
    public Object onCreated(BeanCreatedEvent<Object> event) {
        Object bean = event.getBean();
        // only a bean Micronaut generated a proxy of carries advice, and only that advice holds interceptor
        // instances; anything else is not intercepted at all
        if (bean instanceof Intercepted intercepted) {
            InterceptorInstances targetInstances = instancesOfTarget(bean);
            for (BeanRegistration<Interceptor<?, ?>> registration : intercepted.$interceptorRegistrations()) {
                if (registration.getBean() instanceof JakartaInterceptorAdvice advice) {
                    if (targetInstances != null) {
                        advice.shareInterceptorInstances(targetInstances);
                    }
                    advice.createInterceptorInstances(event.getBeanDefinition());
                }
            }
        }
        // what was not taken by now is nobody's to take, except the instances of a target whose proxy comes next
        InterceptorInstances.forgetPostConstructedExcept(isProxyTarget(event.getBeanDefinition()) ? bean : null);
        return bean;
    }

    /**
     * Finds the interceptor instances the construction and lifecycle of the target of a proxy were intercepted with.
     *
     * <p>Only a proxy that already holds its target is looked at: that target was resolved as the proxy was
     * constructed, on this thread, just now. A proxy that resolves its target lazily has none yet, and asking for it
     * here would create it early; one whose target may be swapped would go on sharing the instances of a target it
     * no longer has.</p>
     *
     * @param proxy The proxy being created
     * @return The instances, or {@code null} when the proxy has no target yet or its target has none
     */
    private static @Nullable InterceptorInstances instancesOfTarget(Object proxy) {
        if (proxy instanceof InterceptedProxy<?> interceptedProxy
            && !(proxy instanceof HotSwappableInterceptedProxy<?>)
            && interceptedProxy.hasCachedInterceptedTarget()) {
            return InterceptorInstances.takePostConstructed(interceptedProxy.interceptedTarget());
        }
        return null;
    }

    /**
     * Whether a bean is created to be the target of a proxy, which is then created around it.
     */
    private static boolean isProxyTarget(BeanDefinition<?> definition) {
        BeanDefinition<?> target = definition;
        while (target instanceof DelegatingBeanDefinition<?> delegating) {
            target = delegating.getTarget();
        }
        return target instanceof BeanDefinitionReference<?> reference && reference.isProxyTarget();
    }
}
