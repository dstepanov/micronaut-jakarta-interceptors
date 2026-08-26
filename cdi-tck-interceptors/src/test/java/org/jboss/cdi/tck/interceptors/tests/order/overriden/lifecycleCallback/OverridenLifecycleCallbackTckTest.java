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
package org.jboss.cdi.tck.interceptors.tests.order.overriden.lifecycleCallback;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The overridden lifecycle callback scenarios of the technology compatibility kit, with the assertions of its own
 * {@code OverridenLifecycleCallbackInterceptorTest}: a callback that is overridden is not invoked, whether or not
 * the method overriding it is a callback of its own.
 *
 * <p>Only the post-construct half is asserted here. Micronaut does not invoke the pre-destroy method a bean
 * declares on itself once the bean has pre-destroy interception, so the destroy counters stay at zero for a reason
 * that has nothing to do with overriding.</p>
 */
class OverridenLifecycleCallbackTckTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @Test
    void testCallbackOverridenByCallback() {
        Bird.reset();
        Eagle.reset();

        Eagle eagle = context.createBean(Eagle.class);
        eagle.ping();

        assertEquals(0, Bird.getInitBirdCalled().get(), "the callback of the superclass is overridden");
        assertEquals(1, Eagle.getInitEagleCalled().get(), "the callback that overrides it is invoked");
    }

    @Test
    void testCallbackOverridenByNonCallback() {
        Bird.reset();
        Falcon.reset();

        Falcon falcon = context.createBean(Falcon.class);
        falcon.ping();

        assertEquals(0, Bird.getInitBirdCalled().get(), "the callback of the superclass is overridden");
        assertEquals(0, Falcon.getInitFalconCalled().get(), "the method overriding it is not a callback");
    }
}
