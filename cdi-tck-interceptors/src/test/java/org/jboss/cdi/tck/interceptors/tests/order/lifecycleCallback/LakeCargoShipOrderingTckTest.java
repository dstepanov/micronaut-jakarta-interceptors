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
package org.jboss.cdi.tck.interceptors.tests.order.lifecycleCallback;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The post-construct ordering scenario of the technology compatibility kit, with the assertion of its own
 * {@code PostConstructOrderTest}.
 *
 * <p>Each interceptor of the chain sets a number and the next one asserts it, so the order the callbacks are
 * invoked in is checked by the scenario itself.</p>
 */
class LakeCargoShipOrderingTckTest {

    @Test
    void interposesOnThePostConstructCallbacksInTheOrderTheKitExpects() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(LakeCargoShip.class);

            assertEquals(7, LakeCargoShip.getSequence());
        }
    }
}
