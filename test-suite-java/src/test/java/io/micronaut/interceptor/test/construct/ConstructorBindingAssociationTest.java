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
import jakarta.inject.Singleton;
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
import java.util.List;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3: the interceptor instances associated with the class of an object exist before any {@code @AroundConstruct}
 * method runs. A constructor that declares a binding of a type its class declares replaces the binding of the class
 * for its own chain; the interceptor the class binding binds still interposes on the business methods, is still
 * associated with the class, and is created before the construction is interposed on.
 */
class ConstructorBindingAssociationTest {

    static final List<String> EVENTS = new ArrayList<>();

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, CONSTRUCTOR, METHOD})
    public @interface Region {
        String value();
    }

    @Interceptor
    @Region("business")
    public static class BusinessRegionInterceptor {
        public BusinessRegionInterceptor() {
            EVENTS.add("business interceptor created");
        }

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            EVENTS.add("business interceptor invoked");
            return context.proceed();
        }
    }

    @Interceptor
    @Region("construction")
    public static class ConstructionRegionInterceptor {
        @AroundConstruct
        public Object construct(InvocationContext context) throws Exception {
            EVENTS.add("around construct");
            return context.proceed();
        }
    }

    @Singleton
    @Region("business")
    public static class RegionalService {
        @Region("construction")
        public RegionalService() {
            EVENTS.add("constructed");
        }

        public String work() {
            return "worked";
        }
    }

    @Test
    void theInterceptorOfTheClassBindingExistsBeforeTheConstructionIsInterposedOn() {
        EVENTS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("worked", context.getBean(RegionalService.class).work());
        }
        assertEquals(List.of(
            "business interceptor created",
            "around construct",
            "constructed",
            "business interceptor invoked"), EVENTS);
    }
}
