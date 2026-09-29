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
import io.micronaut.context.event.BeanDestroyedEvent;
import io.micronaut.context.event.BeanDestroyedEventListener;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

/**
 * Destroys the interceptor instances of an object once the object has been destroyed, for the objects whose
 * pre-destroy interception did not.
 *
 * <p>Section 2.3 destroys the interceptor instances of an object when the object is removed, after its pre-destroy
 * interceptor methods have run. Interposing on the pre-destroy event is therefore where they normally go, and it is
 * also where an interceptor instance may last be needed. That interception is not guaranteed to run: an ordinary
 * Micronaut interceptor of the same event, ordered before the Jakarta advice, may return without proceeding, and then
 * none of the Jakarta pre-destroy interceptor methods run. The object is destroyed all the same, which is what this
 * listener sees, and what is left of its interceptor instances goes then.</p>
 *
 * <p>Micronaut reports a bean as destroyed once everything it owned has been destroyed too, the advice holding these
 * instances among it, so this is the last word on them rather than the one that usually has it. Destroying them twice
 * destroys them once.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Singleton
@Internal
final class InterceptorDestructionListener implements BeanDestroyedEventListener<Object> {

    @Override
    public void onDestroyed(BeanDestroyedEvent<Object> event) {
        // only a bean Micronaut generated a proxy of carries advice, and only that advice holds interceptor
        // instances; anything else is not intercepted at all
        if (event.getBean() instanceof Intercepted intercepted) {
            for (BeanRegistration<Interceptor<?, ?>> registration : intercepted.$interceptorRegistrations()) {
                if (registration.getBean() instanceof JakartaInterceptorAdvice advice) {
                    advice.beanDestroyed();
                }
            }
        }
    }
}
