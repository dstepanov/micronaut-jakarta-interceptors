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
package io.micronaut.interceptor.tck;

import io.micronaut.context.annotation.Factory;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import java.lang.reflect.Proxy;

/**
 * A stand-in for the {@code BeanManager} of Jakarta Contexts and Dependency Injection.
 *
 * <p>Several scenarios of the technology compatibility kit take a {@code BeanManager} as a constructor argument,
 * and the interceptor that interposes on that constructor checks that the argument it was handed is one. What they
 * are testing is the interception, not the bean manager, so a stand-in that satisfies the injection is enough to
 * let those scenarios run. It implements nothing: every call on it fails, which keeps a scenario that really uses
 * a bean manager from passing quietly.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Factory
public class StubBeanManagerFactory {

    /**
     * @return A bean manager that exists but does nothing
     */
    @Singleton
    public BeanManager beanManager() {
        return (BeanManager) Proxy.newProxyInstance(
            BeanManager.class.getClassLoader(),
            new Class<?>[]{BeanManager.class},
            (proxy, method, args) -> {
                if ("toString".equals(method.getName())) {
                    return "stub BeanManager";
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
                throw new UnsupportedOperationException("The harness of this repository stands a BeanManager in "
                    + "for the injection alone; [" + method.getName() + "] is not something it can answer");
            });
    }
}
