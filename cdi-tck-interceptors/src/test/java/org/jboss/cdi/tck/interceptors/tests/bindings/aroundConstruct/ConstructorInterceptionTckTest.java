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
package org.jboss.cdi.tck.interceptors.tests.bindings.aroundConstruct;

import io.micronaut.context.ApplicationContext;
import org.jboss.cdi.tck.util.ActionSequence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The constructor binding scenarios of the technology compatibility kit, with the assertions of its own
 * {@code ConstructorInterceptionTest}.
 */
class ConstructorInterceptionTckTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    private static List<String> sequenceOf(Class<?> bean) {
        ActionSequence.reset();
        context.getBean(bean);
        return ActionSequence.getSequence().getData();
    }

    @Test
    void testConstructorLevelBinding() {
        assertEquals(List.of("AlphaInterceptor2", "BeanWithConstructorLevelBinding"),
            sequenceOf(BeanWithConstructorLevelBinding.class));
    }

    @Test
    void testMultipleConstructorLevelBinding() {
        assertEquals(List.of("AlphaInterceptor2", "BravoInterceptor", "BeanWithMultipleConstructorLevelBinding"),
            sequenceOf(BeanWithMultipleConstructorLevelBinding.class));
    }

    @Test
    void testTypeLevelBinding() {
        assertEquals(List.of("AlphaInterceptor1", "BeanWithTypeLevelBinding"),
            sequenceOf(BeanWithTypeLevelBinding.class));
    }

    @Test
    void testTypeLevelAndConstructorLevelBinding() {
        assertEquals(List.of("AlphaInterceptor1", "BravoInterceptor", "BeanWithConstructorLevelAndTypeLevelBinding"),
            sequenceOf(BeanWithConstructorLevelAndTypeLevelBinding.class));
    }

    @Test
    void testOverridingTypeLevelBinding() {
        assertEquals(List.of("AlphaInterceptor2", "BeanOverridingTypeLevelBinding"),
            sequenceOf(BeanOverridingTypeLevelBinding.class));
    }
}
