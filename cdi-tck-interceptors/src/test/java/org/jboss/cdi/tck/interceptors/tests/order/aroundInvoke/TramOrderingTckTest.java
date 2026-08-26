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
package org.jboss.cdi.tck.interceptors.tests.order.aroundInvoke;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The around-invoke ordering scenario of the technology compatibility kit, with the assertions of its own
 * {@code AroundInvokeOrderTest}.
 *
 * <p>{@code Tram} extends {@code RailVehicle} extends {@code Vehicle}, each declaring an {@code @AroundInvoke}
 * method, and five interceptor classes are bound to it with priorities of their own. Every one of those methods
 * asserts the number it was handed before adding one to it, so the order is checked by the scenario itself.</p>
 */
class TramOrderingTckTest {

    @Test
    void invokesTheInterceptorsInTheOrderTheKitExpects() {
        try (ApplicationContext context = ApplicationContext.run()) {
            // Interceptor1..5, then Vehicle.intercept, RailVehicle.intercept2, Tram.intercept3
            assertEquals(8, context.getBean(Tram.class).getId());

            assertFalse(OverridenInterceptor.isOverridenMethodCalled(),
                "an interceptor method that is overridden is not invoked");
        }
    }
}
