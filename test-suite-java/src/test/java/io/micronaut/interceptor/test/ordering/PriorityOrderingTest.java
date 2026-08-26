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
package io.micronaut.interceptor.test.ordering;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityOrderingTest {

    @Test
    void invokesTheInterceptorsInPriorityOrder() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();

            assertEquals("done", context.getBean(TracedService.class).work());
            assertEquals(
                List.of("first in", "second in", "target", "second out", "first out"),
                Calls.RECORDED);
        }
    }
}
