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
package io.micronaut.interceptor.test.factory;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A factory method is not a business method of its factory but the definition of another bean, and what it binds
 * binds the bean it produces.
 */
class ProducerMethodsTest {

    @Test
    void aBeanAStaticFactoryMethodProducesIsInterceptedByWhatThatMethodBinds() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            Cog cog = context.getBean(Cog.class);

            assertEquals("cog", cog.work());
            assertEquals(List.of("green post", "green invoke", "cog"), Calls.RECORDED);
        }
    }

    @Test
    void aBeanProducedByAFactoryThatInterposesOnItselfIsInterceptedByWhatTheMethodBinds() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            Bolt bolt = context.getBean(Bolt.class);

            // the interceptor method of the factory is a method of the factory, and is not invoked on the product.
            // The factory is intercepted in its own right, which is what has Micronaut intercept the producer
            // method as well: it is a method of the factory, and the binding it carries is the one it declares for
            // the bean, so producing the bean is intercepted once before the bean itself is
            assertEquals("bolt", bolt.work());
            assertEquals(List.of("green invoke", "green post", "green invoke", "bolt"), Calls.RECORDED);
        }
    }

    @Test
    void andThatFactoryStillInterposesOnItsOwnBusinessMethods() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();

            assertEquals("factory", context.getBean(SelfInterceptingBoltFactory.class).describe());
            assertEquals(List.of("factory self describe", "describe"), Calls.RECORDED);
        }
    }
}
