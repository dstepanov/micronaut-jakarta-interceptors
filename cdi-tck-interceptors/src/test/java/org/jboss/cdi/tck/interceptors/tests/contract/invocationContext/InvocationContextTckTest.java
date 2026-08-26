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
package org.jboss.cdi.tck.interceptors.tests.contract.invocationContext;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code InvocationContext} contract of the technology compatibility kit, with the assertions of its own
 * {@code InvocationContextTest}. The scenario checks most of what it asserts from inside the interceptors and
 * records the outcome, which is what the flags read here are.
 */
class InvocationContextTckTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    private static SimpleBean bean() {
        return context.getBean(SimpleBean.class);
    }

    @Test
    void testGetTargetMethod() {
        SimpleBean instance = bean();
        instance.setId(10);
        assertEquals(10, instance.getId());
        assertEquals(10, Interceptor1.getTarget().getId());
    }

    @Test
    void testGetTimerMethod() {
        assertTrue(bean().testGetTimer());
    }

    @Test
    void testGetMethodForAroundInvokeInterceptorMethod() {
        assertTrue(bean().testGetMethod());
    }

    @Test
    void testGetMethodForLifecycleCallbackInterceptorMethod() {
        bean();
        assertTrue(PostConstructInterceptor.isGetMethodReturnsNull());
    }

    @Test
    void testCtxProceedForLifecycleCallbackInterceptorMethod() {
        bean();
        assertTrue(PostConstructInterceptor.isCtxProceedReturnsNull());
    }

    @Test
    void testMethodParameters() {
        assertEquals(5, bean().add(1, 2));
    }

    @Test
    void testIllegalNumberOfParameters() {
        assertThrows(IllegalArgumentException.class, () -> bean().add2(1, 1));
    }

    @Test
    void testIllegalTypeOfParameters() {
        assertThrows(IllegalArgumentException.class, () -> bean().add3(1, 1));
    }

    @Test
    void testProceedReturnsNullForVoidMethod() {
        bean().voidMethod();
        assertTrue(Interceptor7.isProceedReturnsNull());
    }

    @Test
    void testContextData() {
        bean().foo();
        assertTrue(Interceptor8.isContextDataOK());
        assertTrue(Interceptor9.isContextDataOK());
    }

    /**
     * 2.4 n) and o) of the specification, as the kit asserts them: every binding annotation in effect is returned,
     * including the ones that bind an interceptor of another kind and the ones that bind no interceptor at all,
     * and each is equal to the annotation literal of its type.
     */
    @Test
    void testGetInterceptorBindings() {
        assertTrue(bean().bindings());

        assertEquals(Set.of(new SimplePCBinding.Literal(), new PseudoBinding.Literal(),
                new AroundConstructBinding1.Literal(), new AroundConstructBinding2.Literal(),
                new Binding16.Literal("class-level"), new SuperBinding.Literal()),
            AroundConstructInterceptor1.getAllBindings());

        assertEquals(AroundConstructInterceptor2.getAllBindings(), AroundConstructInterceptor1.getAllBindings());

        assertEquals(Set.of(new SimplePCBinding.Literal(), new PseudoBinding.Literal(),
                new AroundConstructBinding1.Literal(), new Binding16.Literal("class-level"),
                new SuperBinding.Literal()),
            PostConstructInterceptor.getAllBindings());

        assertEquals(Set.of(new SimplePCBinding.Literal(), new PseudoBinding.Literal(),
                new AroundConstructBinding1.Literal(), new Binding11.Literal(), new Binding12.Literal(),
                new Binding13.Literal("ko"), new Binding14.Literal("foobar"), new Binding15.Literal(),
                new Binding15Additional.Literal("AdditionalBinding"), new Binding16.Literal("method-level"),
                new SuperBinding.Literal()),
            Interceptor12.getAllBindings());

        assertEquals(Set.of(new Binding12.Literal()), Interceptor12.getBinding12s());
        assertEquals(new Binding12.Literal(), Interceptor12.getBinding12());
        assertEquals(Set.of(), Interceptor12.getBinding5s());
        assertNull(Interceptor12.getBinding6());
    }

    @Test
    void testBusinessMethodNotCalledWithoutProceedInvocation() {
        assertEquals("foo", bean().echo("foo"));
        assertFalse(SimpleBean.isEchoCalled());
    }
}
