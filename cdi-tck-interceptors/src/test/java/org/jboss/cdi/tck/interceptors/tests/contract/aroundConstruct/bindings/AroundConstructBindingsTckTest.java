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
package org.jboss.cdi.tck.interceptors.tests.contract.aroundConstruct.bindings;

import io.micronaut.context.ApplicationContext;
import org.jboss.cdi.tck.util.ActionSequence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The around-construct scenarios of the technology compatibility kit, with the assertions of its own
 * {@code AroundConstructTest}, where the interceptors are bound by an annotation.
 */
class AroundConstructBindingsTckTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @Test
    void testInterceptorInvocation() {
        ActionSequence.reset();
        context.getBean(Alpha.class);
        assertEquals(List.of("AlphaInterceptor"), ActionSequence.getSequence().getData());
    }

    @Test
    void testReplacingParameters() {
        ActionSequence.reset();
        Bravo bravo = context.getBean(Bravo.class);
        assertNotNull(bravo.getParameter());
        assertEquals(BravoInterceptor.NEW_PARAMETER_VALUE, bravo.getParameter().getValue());
        assertEquals(List.of("BravoInterceptor"), ActionSequence.getSequence().getData());
    }

    @Test
    void testExceptions() {
        ActionSequence.reset();

        // Micronaut reports whatever the construction of a bean threw wrapped in an exception of its own, so what
        // the scenario threw is the cause rather than the exception itself
        RuntimeException failure = assertThrows(RuntimeException.class, () -> context.getBean(Charlie.class));
        assertInstanceOf(CharlieException.class, rootCause(failure));

        // reverse order because the interceptors record themselves after proceed returns
        assertEquals(List.of("CharlieInterceptor2", "CharlieInterceptor1"), ActionSequence.getSequence().getData());
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
