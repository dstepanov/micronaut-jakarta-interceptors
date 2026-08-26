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
package org.jboss.cdi.tck.interceptors.tests.contract.exceptions.aroundInvoke;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exception scenarios of the technology compatibility kit, with the assertions of its own {@code ExceptionTest}.
 */
class ExceptionTckTest {

    @Test
    void testExceptions1() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(context.getBean(SimpleBean.class).foo());
        }
    }

    @Test
    void testExceptions2() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(context.getBean(ExceptionBean.class).bar());
        }
    }
}
