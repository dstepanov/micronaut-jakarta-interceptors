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
import io.micronaut.core.annotation.Order;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.spi.BoundInterceptorEnablement;
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

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Listed {
    }

    @Listed
    @Interceptor
    public static class AlphaListedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "alpha " + context.proceed();
        }
    }

    @Listed
    @Interceptor
    public static class ZuluListedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "zulu " + context.proceed();
        }
    }

    @Listed
    @Interceptor
    @jakarta.annotation.Priority(Interceptor.Priority.APPLICATION + 500)
    public static class PrioritizedListedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "prioritized " + context.proceed();
        }
    }

    @Singleton
    @Listed
    public static class ListedService {
        public String work() {
            return "worked";
        }
    }

    static final String LISTING = "test.bound-interceptor-enablement.listing";

    /**
     * Lists two interceptor classes against the order of their names, where the test asks for it. It is ordered
     * before {@link PositioningEnablement}, so that where both are there it is the one asked first.
     */
    @Singleton
    @Order(1)
    @Requires(property = LISTING, value = "true")
    public static class ListingEnablement implements BoundInterceptorEnablement {
        @Override
        public boolean isEnabled(BeanDefinition<?> interceptor) {
            return true;
        }

        @Override
        public int position(BeanDefinition<?> interceptor) {
            if (interceptor.getBeanType() == ZuluListedInterceptor.class) {
                return 0;
            }
            return interceptor.getBeanType() == AlphaListedInterceptor.class ? 1 : -1;
        }
    }

    static final String POSITIONS = "test.bound-interceptor-enablement.positions";

    /**
     * Lists the interceptor classes of this test at the positions the test asks for: {@code tie} lists the two
     * named ones at one position, {@code all} lists every one, the prioritized one at the last position there is.
     */
    @Singleton
    @Order(2)
    @Requires(property = POSITIONS)
    public static class PositioningEnablement implements BoundInterceptorEnablement {

        private final String positions;

        PositioningEnablement(@io.micronaut.context.annotation.Value("${" + POSITIONS + "}") String positions) {
            this.positions = positions;
        }

        @Override
        public boolean isEnabled(BeanDefinition<?> interceptor) {
            return true;
        }

        @Override
        public int position(BeanDefinition<?> interceptor) {
            Class<?> type = interceptor.getBeanType();
            if (type == ZuluListedInterceptor.class || type == AlphaListedInterceptor.class) {
                return 0;
            }
            return type == PrioritizedListedInterceptor.class && positions.equals("all") ? Integer.MAX_VALUE : -1;
        }
    }

    @Test
    void theInterceptorsListedAtOnePositionKeepTheOrderOfTheirPriorityAndName() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(POSITIONS, "tie"))) {
            assertEquals("prioritized alpha zulu worked", context.getBean(ListedService.class).work());
        }
    }

    @Test
    void whenEveryInterceptorIsListedTheListAloneOrdersThem() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(POSITIONS, "all"))) {
            assertEquals("alpha zulu prioritized worked", context.getBean(ListedService.class).work());
        }
    }

    @Test
    void boundInterceptorsAreOrderedByPriorityThenNameOnTheirOwn() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("alpha zulu prioritized worked", context.getBean(ListedService.class).work());
        }
    }

    @Test
    void theInterceptorsAModuleListsComeAfterTheOnesOrderedByPriorityInTheOrderOfTheList() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(LISTING, "true"))) {
            assertEquals("prioritized zulu alpha worked", context.getBean(ListedService.class).work());
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

    /**
     * Where several modules provide one, an interceptor class is enabled only where every one of them enables it,
     * and is at the position the first of them to list it gives it, in the order the beans are ordered by.
     */
    @Test
    void severalModulesEnableAnInterceptorTogetherAndTheFirstToListItPlacesIt() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(DISABLING, "true", LISTING, "true",
            POSITIONS, "tie"))) {
            assertEquals("worked", context.getBean(SwitchedService.class).work());
            assertEquals("prioritized zulu alpha worked", context.getBean(ListedService.class).work());
        }
    }

    @Test
    void anInterceptorNamedByTheElementIsEnabledByBeingNamed() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(DISABLING, "true"))) {
            assertEquals("switched worked", context.getBean(NamingService.class).work());
        }
    }

    /**
     * A method keeps only what its compiled metadata records, and the chain resolved from it stays with the resolver
     * of each context: two contexts open at once in one virtual machine, enabling differently, each keep the chains
     * of their own enablement, even where they share the methods of a proxy.
     */
    @Test
    void twoContextsOpenAtOnceEachKeepTheChainsOfTheirOwnEnablement() {
        try (ApplicationContext disabling = ApplicationContext.run(Map.of(DISABLING, "true"));
             ApplicationContext enabling = ApplicationContext.run()) {
            assertEquals("worked", disabling.getBean(SwitchedService.class).work());
            assertEquals("switched worked", enabling.getBean(SwitchedService.class).work());
            assertEquals("worked", disabling.getBean(SwitchedService.class).work());
            assertEquals("switched worked", enabling.getBean(SwitchedService.class).work());
        }
    }
}
