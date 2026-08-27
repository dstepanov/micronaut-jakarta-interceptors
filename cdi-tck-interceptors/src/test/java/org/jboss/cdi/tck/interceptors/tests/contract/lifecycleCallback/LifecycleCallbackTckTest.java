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
package org.jboss.cdi.tck.interceptors.tests.contract.lifecycleCallback;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lifecycle callback scenarios of the technology compatibility kit, with the assertions of its own
 * {@code LifecycleCallbackInterceptorTest}.
 */
class LifecycleCallbackTckTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    private static <T> void createAndDestroyInstance(Class<T> type) {
        T bean = context.createBean(type);
        context.destroyBean(bean);
    }

    @Test
    void testPostConstructInterceptor() {
        context.getBean(Goat.class);
        assertTrue(Goat.isPostConstructInterceptorCalled());
        assertTrue(AnimalInterceptor.isPostConstructInterceptorCalled(Goat.GOAT));
        context.getBean(Hen.class).toString();
        assertTrue(Hen.isPostConstructInterceptorCalled());
        assertTrue(AnimalInterceptor.isPostConstructInterceptorCalled(Hen.HEN));
        context.getBean(Cow.class).toString();
        assertTrue(Cow.isPostConstructInterceptorCalled());
        assertTrue(AnimalInterceptor.isPostConstructInterceptorCalled(Cow.COW));
    }

    @Test
    void testPreDestroyInterceptor() {
        createAndDestroyInstance(Goat.class);
        assertTrue(AnimalInterceptor.isPreDestroyInterceptorCalled(Goat.GOAT));
        createAndDestroyInstance(Hen.class);
        assertTrue(AnimalInterceptor.isPreDestroyInterceptorCalled(Hen.HEN));
        createAndDestroyInstance(Cow.class);
        assertTrue(AnimalInterceptor.isPreDestroyInterceptorCalled(Cow.COW));
    }

    @Test
    void testPreDestroyCallbackOfTheBeanItself() {
        createAndDestroyInstance(Goat.class);
        assertTrue(Goat.isPreDestroyInterceptorCalled());
    }

    @Test
    void testSingleMethodInterposingMultipleLifecycleCallbackEvents() {
        AlmightyLifecycleInterceptor.reset();
        Dog.reset();
        createAndDestroyInstance(Dog.class);
        // one interceptor method interposes on the construction and on both callbacks of the object
        assertEquals(3, AlmightyLifecycleInterceptor.getNumberOfInterceptions());
    }

    @Test
    void testSingleMethodOfTheBeanInterposingOnBothOfItsCallbacks() {
        AlmightyLifecycleInterceptor.reset();
        Dog.reset();
        createAndDestroyInstance(Dog.class);
        assertEquals(2, Dog.getNumberOfInterceptions());
    }

    @Test
    void testAroundInvokeAndLifeCycleCallbackInterceptorsCanBeDefinedOnTheSameClass() {
        assertEquals("foofoo", context.getBean(Goat.class).echo("foo"));
    }

    @Test
    void testPublicLifecycleInterceptorMethod() {
        context.getBean(Chicken.class);
        assertTrue(PublicLifecycleInterceptor.isIntercepted());
    }

    @Test
    void testProtectedLifecycleInterceptorMethod() {
        context.getBean(Chicken.class);
        assertTrue(ProtectedLifecycleInterceptor.isIntercepted());
    }

    @Test
    @Disabled("The specification allows a private interceptor method; this module rejects one, because it is "
        + "invoked through the executable method Micronaut generates for it and a private method has none. The "
        + "scenario declaring it is not built, and the difference is recorded under Conformance, section 2.7 jb)")
    void testPrivateLifecycleInterceptorMethod() {
    }

    @Test
    void testPackagePrivateLifecycleInterceptorMethod() {
        context.getBean(Chicken.class);
        assertTrue(PackagePrivateLifecycleInterceptor.isIntercepted());
    }

    @Test
    void testLifeCycleCallbackInterceptorNotInvokedForMethodLevelInterceptor() {
        assertEquals("bar", context.getBean(Sheep.class).foo());
        assertTrue(SheepInterceptor.isAroundInvokeCalled());
        assertFalse(SheepInterceptor.isPostConstructCalled());
    }
}
