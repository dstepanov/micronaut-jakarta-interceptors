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
package org.jboss.cdi.tck.interceptors.tests.contract.interceptorLifeCycle.aroundConstruct.withAroundInvoke;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One interceptor instance serves the construction and the business methods of one object, with the assertion of
 * the kit's own {@code SingleInterceptorInstanceTest}.
 */
class SingleInterceptorInstanceTckTest {

    @Test
    void testOneInterceptorInstancePerInterceptedObject() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals(2, context.getBean(Foo.class).ping());
        }
    }
}
