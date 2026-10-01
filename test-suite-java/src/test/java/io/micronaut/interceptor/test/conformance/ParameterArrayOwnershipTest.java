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
package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Micronaut owns the parameter array; only an explicit, validated setter replaces its values.
 * Array ownership is an implementation policy, not a Jakarta Interceptors aliasing requirement.
 */
class ParameterArrayOwnershipTest {

    enum Operation {
        MODIFY_GETTER, MODIFY_SETTER, REJECT_INVALID_SETTER
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void methodArgumentsAreOwnedAndReplacementsAreAtomic(Operation operation) {
        try (ApplicationContext context = ApplicationContext.run()) {
            String expected = operation == Operation.MODIFY_SETTER ? "replacement" : "original";
            assertEquals(expected, context.getBean(MethodTarget.class).echo(operation.name(), "original"));
        }
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void constructorArgumentsAreOwnedAndReplacementsAreAtomic(Operation operation) {
        try (ApplicationContext context = ApplicationContext.run(Map.of("ownership.mode", operation.name()))) {
            String expected = operation == Operation.MODIFY_SETTER ? "replacement" : "original";
            assertEquals(expected, context.getBean(ConstructorTarget.class).value);
        }
    }

    private static void exercise(InvocationContext invocation) {
        Object[] initial = invocation.getParameters();
        Operation operation = Operation.valueOf((String) initial[0]);
        switch (operation) {
            case MODIFY_GETTER -> {
                initial[1] = "getter mutation";
                assertArrayEquals(new Object[]{operation.name(), "original"}, invocation.getParameters());
            }
            case MODIFY_SETTER -> {
                Object[] replacement = {operation.name(), "replacement"};
                invocation.setParameters(replacement);
                assertArrayEquals(replacement, invocation.getParameters());
                assertNotSame(replacement, invocation.getParameters());
                replacement[1] = "later mutation";
                assertArrayEquals(new Object[]{operation.name(), "replacement"}, invocation.getParameters());
            }
            case REJECT_INVALID_SETTER -> {
                assertThrows(IllegalArgumentException.class,
                    () -> invocation.setParameters(new Object[]{"must not be written", 42}));
                assertArrayEquals(initial, invocation.getParameters());
                assertThrows(IllegalArgumentException.class, () -> invocation.setParameters(new Object[0]));
                assertArrayEquals(initial, invocation.getParameters());
                assertThrows(IllegalArgumentException.class, () -> invocation.setParameters(null));
                assertArrayEquals(initial, invocation.getParameters());
            }
        }
    }

    @Singleton
    @Interceptors(MethodInterceptor.class)
    public static class MethodTarget {
        public String echo(String operation, String value) {
            return value;
        }
    }

    public static class MethodInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext invocation) throws Exception {
            exercise(invocation);
            return invocation.proceed();
        }
    }

    @Singleton
    @Interceptors(ConstructorInterceptor.class)
    public static class ConstructorTarget {
        final String value;

        @Inject
        public ConstructorTarget(@Value("${ownership.mode}") String operation,
                                 @Value("${ownership.value:original}") String value) {
            this.value = value;
        }
    }

    public static class ConstructorInterceptor {
        @AroundConstruct
        public Object intercept(InvocationContext invocation) throws Exception {
            exercise(invocation);
            return invocation.proceed();
        }
    }
}
