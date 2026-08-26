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
package org.jboss.cdi.tck.interceptors.tests.bindings.members;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The binding member scenario of the technology compatibility kit, with the assertions of its own
 * {@code InterceptorBindingTypeWithMemberTest}.
 */
class BindingMembersTckTest {

    @Test
    void bindsByTheMemberValueOfTheBinding() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Farm farm = context.getBean(Farm.class);

            assertEquals(20, farm.getAnimalCount());
            assertTrue(IncreasingInterceptor.isIntercepted(), "the interceptor of the declared value intercepts");
            assertFalse(DecreasingInterceptor.isIntercepted(), "the interceptor of another value does not");
        }
    }

    @Test
    void ignoresAMemberExcludedFromTheBinding() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals(20, context.getBean(Farm.class).getVehicleCount());
            assertTrue(VehicleCountInterceptor.isIntercepted());
        }
    }
}
