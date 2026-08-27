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
package org.jboss.cdi.tck.interceptors.tests.bindings.resolution;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The binding resolution scenarios of the technology compatibility kit, with the assertions of its own
 * {@code InterceptorBindingResolutionTest}.
 *
 * <p>Each of that test's methods asks a {@code jakarta.enterprise.inject.spi.BeanManager} how many interceptors a
 * set of bindings resolves to, and then checks which interceptor actually ran. There is no bean manager here to
 * ask, so the same question is put to the interception rather than to the container: the binding set each of those
 * counts is taken over is the binding set of an element of this deployment, so a count of one is that the one
 * interceptor interposed and that neither of the other two did. Both halves are asserted here.</p>
 *
 * <p>What the count discriminates is worth stating, because the three interceptor classes of the deployment are
 * alike: each declares {@code @TransactionalBinding} and {@code @LoggedBinding} and differs in one binding and in
 * the kind of interceptor method it declares. So a wrongly widened match, or an interceptor method invoked for the
 * wrong interception type, shows up as a second interceptor having interposed.</p>
 *
 * <p>The bindings these scenarios resolve by are as involved as the kit has. {@code ComplicatedInterceptor} binds
 * on five annotations at once, and a service only matches them by collecting them from everywhere a binding can
 * come from: {@code @TransactionalBinding} through a stereotype on a grandparent class, {@code @LoggedBinding}
 * inherited from a parent, {@code @MessageBinding} on the class itself, {@code @PingBinding} on the method, and
 * {@code @BallBinding} through {@code @PongBinding}, which {@code @PingBinding} carries. That last one matches
 * although the two declarations differ, because they differ only in a member marked
 * {@code jakarta.enterprise.util.Nonbinding}.</p>
 */
class BindingResolutionTckTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    /**
     * A business method whose bindings are gathered from a stereotype, two superclasses, the class and the method,
     * against a service that is missing one of them.
     */
    @Test
    void testBusinessMethodInterceptorBindings() {
        MessageService messageService = context.getBean(MessageService.class);
        assertNotNull(messageService);
        reset();
        messageService.ping();
        assertTrue(ComplicatedInterceptor.intercepted);
        // the count: the around-invoke interceptor and no other
        assertNoOtherInterceptorInterposed();

        MonitorService monitorService = context.getBean(MonitorService.class);
        assertNotNull(monitorService);
        reset();
        monitorService.ping();
        // MonitorService declares everything ComplicatedInterceptor binds on except @MessageBinding
        assertFalse(ComplicatedInterceptor.intercepted);
        assertNoOtherInterceptorInterposed();
    }

    /**
     * The lifecycle callbacks of a bean whose bindings are gathered the same way.
     */
    @Test
    void testLifecycleInterceptorBindings() {
        reset();

        RemoteMessageService remoteMessageService = context.createBean(RemoteMessageService.class);
        remoteMessageService.ping();
        context.destroyBean(remoteMessageService);

        assertTrue(ComplicatedLifecycleInterceptor.postConstructCalled);
        assertTrue(ComplicatedLifecycleInterceptor.preDestroyCalled);
        // the count, once for each of the two callbacks: the lifecycle interceptor and no other. Neither of the
        // others interposes, although ComplicatedInterceptor declares every binding this bean carries but
        // @BasketBinding, which is what keeps it out
        assertFalse(ComplicatedInterceptor.intercepted);
        assertFalse(ComplicatedAroundConstructInterceptor.aroundConstructCalled);
    }

    /**
     * The construction of a bean bound by a binding its constructor declares, which carries a second one.
     */
    @Test
    void testConstructorInterceptorBindings() {
        reset();

        assertNotNull(context.createBean(MachineService.class));

        assertTrue(ComplicatedAroundConstructInterceptor.aroundConstructCalled);
        // the count: the around-construct interceptor and no other
        assertFalse(ComplicatedInterceptor.intercepted);
        assertFalse(ComplicatedLifecycleInterceptor.postConstructCalled);
        assertFalse(ComplicatedLifecycleInterceptor.preDestroyCalled);
    }

    private static void reset() {
        ComplicatedInterceptor.reset();
        ComplicatedLifecycleInterceptor.reset();
        ComplicatedAroundConstructInterceptor.reset();
    }

    /**
     * That nothing but the around-invoke interceptor of the deployment interposed.
     */
    private static void assertNoOtherInterceptorInterposed() {
        assertFalse(ComplicatedLifecycleInterceptor.postConstructCalled);
        assertFalse(ComplicatedLifecycleInterceptor.preDestroyCalled);
        assertFalse(ComplicatedAroundConstructInterceptor.aroundConstructCalled);
    }
}
