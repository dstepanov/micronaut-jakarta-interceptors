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
package org.jboss.cdi.tck.interceptors.tests.order.aroundConstruct;

import io.micronaut.context.ApplicationContext;
import org.jboss.cdi.tck.util.ActionSequence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The around-construct ordering scenario of the technology compatibility kit, with the assertion of its own
 * {@code AroundConstructOrderTest}.
 *
 * <p>The interceptor methods of a hierarchy come first, the most general superclass before the class that extends
 * it, and the interceptor classes follow in the order of their priorities.</p>
 */
class FooConstructOrderingTckTest {

    @Test
    void constructsInTheOrderTheKitExpects() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ActionSequence.reset();

            context.getBean(Foo.class);

            assertEquals(
                List.of("SuperInterceptor1", "MiddleInterceptor1", "Interceptor1",
                    "Interceptor2", "Interceptor3", "Interceptor4"),
                ActionSequence.getSequence().getData());
        }
    }
}
