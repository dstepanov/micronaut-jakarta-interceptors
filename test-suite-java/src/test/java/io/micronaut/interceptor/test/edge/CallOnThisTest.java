package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallOnThisTest {

    /**
     * The proxy Micronaut generates by default is a subclass of the bean, and it is the instance the context holds.
     * Inside a business method {@code this} is that proxy, so a call on it to another business method goes through
     * the overriding method of the proxy and is intercepted as a call from outside would be.
     */
    @Test
    void aCallOnThisIsInterceptedWhenTheProxyIsTheBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Log.RECORDED.clear();

            assertEquals("inner", context.getBean(CallingService.class).outer());
            assertEquals(List.of("intercepted outer", "outer body", "intercepted inner", "inner body"), Log.RECORDED);
        }
    }

    /**
     * With {@code @Around(proxyTarget = true)} the proxy holds a separate instance of the bean and delegates to it.
     * Inside a business method {@code this} is that target, which the proxy does not override, so a call on it to
     * another business method is not intercepted.
     */
    @Test
    void aCallOnThisIsNotInterceptedWhenTheProxyWrapsATarget() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Log.RECORDED.clear();

            assertEquals("inner", context.getBean(TargetProxiedCallingService.class).outer());
            assertEquals(List.of("intercepted outer", "outer body", "inner body"), Log.RECORDED);
        }
    }
}
