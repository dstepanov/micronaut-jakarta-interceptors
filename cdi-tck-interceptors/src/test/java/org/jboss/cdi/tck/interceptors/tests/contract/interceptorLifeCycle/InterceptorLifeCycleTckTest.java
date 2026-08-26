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
package org.jboss.cdi.tck.interceptors.tests.contract.interceptorLifeCycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interceptor lifecycle scenarios of the technology compatibility kit, with the assertions of its own
 * {@code InterceptorLifeCycleTest}. The kit creates and destroys its beans through a bean manager; the bean
 * context does the same thing here.
 */
class InterceptorLifeCycleTckTest {

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
    void testInterceptorMethodsCalledAfterDependencyInjection() {
        Baz baz = context.createBean(Baz.class);
        baz.doSomething();
        context.destroyBean(baz);

        // the assertions about the injection are made inside the interceptors
        assertTrue(AroundInvokeInterceptor.called);
        assertTrue(PostConstructInterceptor.called);
        assertTrue(PreDestroyInterceptor.called);
    }

    @Test
    @Disabled("Section 2.3 ba) has an interceptor instance created for each interceptor class when the target "
        + "instance is created. This module creates one when the interceptor is first needed instead, so an "
        + "around-invoke interceptor of a bean that has not been called yet has no instance. Recorded as a "
        + "difference under Conformance, section 2.3 ba)")
    void testInterceptorInstanceCreatedWhenTargetInstanceCreated() {
        Warrior warrior = context.getBean(Warrior.class);

        assertEquals(1, WarriorAIInterceptor.count);
        assertEquals(1, MethodInterceptor.count);
        assertEquals(1, WarriorAttackAIInterceptor.count);
        assertEquals(2, WeaponAIInterceptor.count);
    }

    @Test
    void testOneInterceptorInstancePerTargetInstance() {
        int before = WarriorPCInterceptor.count;
        Warrior warrior = context.getBean(Warrior.class);

        // the post-construct interceptor is needed as the target is created, so it exists by now
        assertEquals(before + 1, WarriorPCInterceptor.count);

        warrior.attack1();
        warrior.attack2();

        // each of the two weapons injected into the warrior has an interceptor instance of its own
        assertNotEquals(warrior.getWeapon1().getWI(), warrior.getWeapon2().getWI());
    }

}
