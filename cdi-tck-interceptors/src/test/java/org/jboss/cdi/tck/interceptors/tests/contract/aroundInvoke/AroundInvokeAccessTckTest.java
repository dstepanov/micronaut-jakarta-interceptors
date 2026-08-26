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
package org.jboss.cdi.tck.interceptors.tests.contract.aroundInvoke;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The access level scenario of the technology compatibility kit, with the assertions of its own
 * {@code AroundInvokeAccessInterceptorTest}.
 *
 * <p>The private case is not here: this module rejects a private interceptor method while it is compiled, so the
 * two scenarios that declare one are not built. That difference is recorded under Conformance.</p>
 */
class AroundInvokeAccessTckTest {

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
    void testProtectedAroundInvokeInterceptor() {
        assertEquals(2, context.getBean(SimpleBean.class).one());
        assertEquals(1, context.getBean(Bean1.class).zero());
    }

    @Test
    void testPackagePrivateAroundInvokeInterceptor() {
        assertEquals(3, context.getBean(SimpleBean.class).two());
        assertEquals(1, context.getBean(Bean2.class).zero());
    }
}
