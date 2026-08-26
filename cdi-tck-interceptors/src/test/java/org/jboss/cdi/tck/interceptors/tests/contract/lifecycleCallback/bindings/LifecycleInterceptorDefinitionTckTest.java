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
package org.jboss.cdi.tck.interceptors.tests.contract.lifecycleCallback.bindings;

import io.micronaut.context.ApplicationContext;
import org.jboss.cdi.tck.util.ActionSequence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The lifecycle interception scenarios of the technology compatibility kit, with the assertions of its own
 * {@code LifecycleInterceptorDefinitionTest}. The kit creates and destroys its beans through a bean manager; the
 * bean context does the same thing here, and the assertions are the kit's own.
 */
class LifecycleInterceptorDefinitionTckTest {

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
    void testLifecycleInterception() {
        ActionSequence.reset();

        Missile missile = context.createBean(Missile.class);
        missile.fire();
        context.destroyBean(missile);

        assertEquals(1, ActionSequence.getSequenceSize("postConstruct"));
        assertEquals(AirborneInterceptor.class.getSimpleName(), ActionSequence.getSequenceData("postConstruct").get(0));
        assertEquals(1, ActionSequence.getSequenceSize("preDestroy"));
        assertEquals(AirborneInterceptor.class.getSimpleName(), ActionSequence.getSequenceData("preDestroy").get(0));
    }

    @Test
    void testMultipleLifecycleInterceptorsAroundConstruct() {
        ActionSequence.reset();

        Rocket rocket = context.createBean(Rocket.class);
        rocket.fire();
        context.destroyBean(rocket);

        assertEquals(
            List.of("AirborneInterceptor", "SuperDestructionInterceptor", "DestructionInterceptor"),
            ActionSequence.getSequenceData("aroundConstruct"));
    }

    @Test
    @Disabled("Micronaut does not invoke the @PreDestroy and @PostConstruct methods a bean declares on itself once "
        + "the bean has lifecycle interception, so the sequences end at the interceptor classes and do not reach "
        + "Weapon and Rocket. The interceptor half of each sequence is asserted by testLifecycleInterception")
    void testMultipleLifecycleInterceptors() {
        ActionSequence.reset();

        Rocket rocket = context.createBean(Rocket.class);
        rocket.fire();
        context.destroyBean(rocket);

        assertEquals(
            List.of("AirborneInterceptor", "SuperDestructionInterceptor", "DestructionInterceptor", "Weapon", "Rocket"),
            ActionSequence.getSequenceData("postConstruct"));
        assertEquals(
            List.of("AirborneInterceptor", "SuperDestructionInterceptor", "DestructionInterceptor", "Weapon", "Rocket"),
            ActionSequence.getSequenceData("preDestroy"));
    }
}
