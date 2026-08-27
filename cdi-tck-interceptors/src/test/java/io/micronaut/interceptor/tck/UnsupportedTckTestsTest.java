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
package io.micronaut.interceptor.tck;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The test classes of the technology compatibility kit whose interceptor scenarios cannot be driven from here at
 * all, each recorded with the reason. They are marked rather than left out, so that what the harness does not
 * cover is as visible in a test report as what it does.
 *
 * <p>The kit's own test classes and assertions run directly for everything else.</p>
 */
class UnsupportedTckTestsTest {

    @Test
    @Disabled("contract/aroundInvoke/AroundInvokeAccessInterceptorTest#testPrivateAroundInvokeInterceptor and "
        + "contract/lifecycleCallback/LifecycleCallbackInterceptorTest#testPrivateLifecycleInterceptorMethod "
        + "declare a private interceptor method. This module rejects one, because it is invoked through the "
        + "executable method Micronaut generates for it and a private method has none. Recorded as a difference "
        + "under Conformance, sections 2.6 cb) and 2.7 jb)")
    void privateInterceptorMethods() {
    }

    @Test
    @Disabled("contract/interceptorLifeCycle/InterceptorLifeCycleTest#"
        + "testInterceptorInstanceCreatedWhenTargetInstanceCreated expects every associated interceptor instance "
        + "to be created with its target. This module creates an interceptor when it is first needed instead, as "
        + "recorded under Conformance, section 2.3 ba)")
    void interceptorInstancesCreatedWithTheirTarget() {
    }
}
