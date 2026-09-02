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
package io.micronaut.interceptor.test.lifecycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An interceptor class that is not a singleton keeps its state per object it interposes on: two objects of the
 * same type each get their own instance, and neither sees what the other's interceptor counted.
 */
class PerTargetStateTest {

    @Test
    void anInterceptorKeepsItsStatePerInterceptedObject() {
        StatefulInterceptor.COUNTS.clear();
        StatefulInterceptor.CONSTRUCTED.set(0);
        try (ApplicationContext context = ApplicationContext.run()) {
            CountedService first = context.createBean(CountedService.class);
            CountedService second = context.createBean(CountedService.class);

            first.work();
            first.work();
            second.work();

            // the first object's interceptor counted to two; the second's started again at one
            assertEquals(List.of(1, 2, 1), StatefulInterceptor.COUNTS);
            assertEquals(2, StatefulInterceptor.CONSTRUCTED.get());
        }
    }
}
