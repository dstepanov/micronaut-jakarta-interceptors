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
package io.micronaut.interceptor.test.ordering;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.runtime.BoundInterceptorEnablement;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * On its own the module enables every interceptor class of the context: one bound by an annotation intercepts without
 * declaring a priority. A module that has rules of enablement provides a {@link BoundInterceptorEnablement}, and the
 * interceptor classes it does not enable are left out of the chains - the ones a binding binds, and never the ones an
 * element names with {@code @Interceptors}.
 */
class BoundInterceptorEnablementTest {

    static final String DISABLING = "test.bound-interceptor-enablement.disabling";

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Switched {
    }

    @Switched
    @Interceptor
    public static class SwitchedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "switched " + context.proceed();
        }
    }

    @Singleton
    @Switched
    public static class SwitchedService {
        public String work() {
            return "worked";
        }
    }

    @Singleton
    @Interceptors(SwitchedInterceptor.class)
    public static class NamingService {
        public String work() {
            return "worked";
        }
    }

    /** Leaves out the one interceptor class of this test, and only where the test asks for it. */
    @Singleton
    @Requires(property = DISABLING, value = "true")
    public static class DisablingEnablement implements BoundInterceptorEnablement {
        @Override
        public boolean isEnabled(BeanDefinition<?> interceptor) {
            return interceptor.getBeanType() != SwitchedInterceptor.class;
        }
    }

    @Test
    void everyBoundInterceptorIsEnabledOnItsOwn() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("switched worked", context.getBean(SwitchedService.class).work());
        }
    }

    @Test
    void anInterceptorThatIsNotEnabledIsLeftOutOfTheChainsItsBindingBinds() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(DISABLING, "true"))) {
            assertEquals("worked", context.getBean(SwitchedService.class).work());
        }
    }

    @Test
    void anInterceptorNamedByTheElementIsEnabledByBeingNamed() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(DISABLING, "true"))) {
            assertEquals("switched worked", context.getBean(NamingService.class).work());
        }
    }
}
