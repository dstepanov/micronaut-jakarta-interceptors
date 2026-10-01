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
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.type.Argument;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterSnapshotTest {

    @Test
    @SuppressWarnings("unchecked")
    void callerMutationDuringValidationCannotChangeTheInstalledSnapshot() {
        Object[] current = {"original"};
        Object[] supplied = {"replacement"};
        MethodInvocationContext<Object, Object> micronaut = (MethodInvocationContext<Object, Object>) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{MethodInvocationContext.class}, (proxy, method, arguments) -> {
                return switch (method.getName()) {
                    case "getParameterValues" -> current;
                    case "isSuspend" -> false;
                    case "getArguments" -> {
                        // Deliberately interleave caller mutation with validation, without a timing-dependent thread.
                        supplied[0] = 42;
                        yield new Argument<?>[]{Argument.of(String.class, "value")};
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
        // The advice and chain are not used by parameter accessors.
        BusinessMethodInvocationContext invocation = new BusinessMethodInvocationContext(micronaut, List.of(), null, null);

        invocation.setParameters(supplied);

        assertEquals(42, supplied[0]);
        assertArrayEquals(new Object[]{"replacement"}, current);
        assertArrayEquals(new Object[]{"replacement"}, invocation.getParameters());
    }
}
