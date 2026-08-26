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
package org.jboss.cdi.tck.interceptors.tests.bindings.multiple;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The multiple binding scenarios of the technology compatibility kit, with the assertions of its own
 * {@code MultipleInterceptorBindingsTest}.
 */
class MultipleBindingsTckTest {

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
    void testInterceptorAppliedToBeanWithAllBindings() {
        MissileInterceptor.intercepted = false;
        context.getBean(FastAndDeadlyMissile.class).fire();
        assertTrue(MissileInterceptor.intercepted);
    }

    @Test
    void testInterceptorNotAppliedToBeanWithSomeBindings() {
        MissileInterceptor.intercepted = false;
        context.getBean(SlowMissile.class).fire();
        assertFalse(MissileInterceptor.intercepted);
    }

    @Test
    void testMultipleInterceptorsOnMethod() {
        LockInterceptor.intercepted = false;
        GuidedMissile bullet = context.getBean(GuidedMissile.class);
        bullet.fire();
        assertFalse(LockInterceptor.intercepted);
        bullet.lockAndFire();
        assertTrue(LockInterceptor.intercepted);
    }
}
