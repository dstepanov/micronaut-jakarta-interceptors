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
package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.runtime.context.scope.ThreadLocal;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3 da): the instance of an interceptor class associated with the class of an object exists before any
 * {@code @AroundConstruct} method runs, even where that interceptor takes no part in the construction, however the
 * object comes to be created.
 *
 * <p>The advice of the object creates every interceptor instance of the object as Micronaut hands it the bean, which
 * it does for a bean looked up, for one created through {@code BeanContext.createBean}, and for the target of a
 * scoped proxy as the proxy first resolves it. The constructor here replaces the binding of its class, so the
 * interceptor of the class binding interposes on the business methods alone, and nothing but the advice creates it
 * before the construction is interposed on.</p>
 */
class AssociatedInstancesCreationPathsTest {

    static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, CONSTRUCTOR, METHOD})
    public @interface Probed {
        String value();
    }

    @Interceptor
    @Probed("business")
    public static class BusinessProbeInterceptor {
        public BusinessProbeInterceptor() {
            EVENTS.add("business interceptor created");
        }

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            EVENTS.add("business interceptor invoked");
            return context.proceed();
        }
    }

    @Interceptor
    @Probed("construction")
    public static class ConstructionProbeInterceptor {
        @AroundConstruct
        public Object construct(InvocationContext context) throws Exception {
            EVENTS.add("around construct");
            return context.proceed();
        }
    }

    @Prototype
    @Probed("business")
    public static class ProbedService {
        @Probed("construction")
        public ProbedService() {
            EVENTS.add("constructed");
        }

        public String work() {
            return "worked";
        }
    }

    @ThreadLocal
    @Probed("business")
    public static class ScopedProbedService {
        @Probed("construction")
        public ScopedProbedService() {
            EVENTS.add("constructed");
        }

        public String work() {
            return "worked";
        }
    }

    private static final List<String> EXPECTED = List.of(
        "business interceptor created",
        "around construct",
        "constructed",
        "business interceptor invoked");

    @Test
    void aBeanLookedUp() {
        EVENTS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("worked", context.getBean(ProbedService.class).work());
        }
        assertEquals(EXPECTED, EVENTS);
    }

    @Test
    void aBeanCreatedThroughCreateBean() {
        EVENTS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("worked", context.createBean(ProbedService.class).work());
        }
        assertEquals(EXPECTED, EVENTS);
    }

    @Test
    void theTargetOfAScopedProxy() {
        EVENTS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("worked", context.getBean(ScopedProbedService.class).work());
        }
        // the proxy Micronaut generates for the scope extends the class, and its own constructor records the first
        // "constructed" without being interposed on; what follows is the creation of the target on the first call
        assertEquals(EXPECTED, EVENTS.subList(EVENTS.indexOf("business interceptor created"), EVENTS.size()));
    }
}
