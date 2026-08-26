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
package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 2.5 bb) of the specification, where the chain has more than one interceptor left to run.
 *
 * <p>An interceptor that recovers by proceeding a second time runs the rest of the chain again, rather than
 * dropping through to the intercepted method and leaving the interceptors between them out.</p>
 */
class RepeatedProceedTest {

    @Test
    void proceedingASecondTimeRunsTheRestOfTheChainAgain() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            RetryingService.attempts = 0;

            assertEquals("value", context.getBean(RetryingService.class).work());

            // the inner interceptor ran on both attempts, not only on the first
            assertEquals(
                List.of("outer", "inner", "target failed", "outer retry", "inner", "target"),
                Calls.RECORDED);
        }
    }
}
