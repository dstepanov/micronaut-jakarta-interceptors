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
import io.micronaut.aop.Interceptor;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

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
            for (BeanRegistration<Interceptor<?, ?>> registration : intercepted.$interceptorRegistrations()) {
                if (registration.getBean() instanceof JakartaInterceptorAdvice advice) {
                    advice.createInterceptorInstances(event.getBeanDefinition());
                }
            }
        }
        return bean;
    }
}
