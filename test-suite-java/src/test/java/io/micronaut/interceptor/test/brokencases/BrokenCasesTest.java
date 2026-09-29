package io.micronaut.interceptor.test.brokencases;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.test.state.PairedInterceptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The cases the guide lists under "Interceptor state on a proxy whose target it does not find": one interceptor
 * instance should serve the post-construct event of the target and the business methods of the proxy.
 */
class BrokenCasesTest {

    @Test
    void lazyProxyTarget() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(LazyTargetService.class).work());
            assertOneInstance("lazy");
        }
        assertDestroyedOnce("lazy");
    }

    @Test
    void hotswapProxyTarget() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(HotswapTargetService.class).work());
            assertOneInstance("hotswap");
        }
        assertDestroyedOnce("hotswap");
    }

    @Test
    void scopedTargetInterceptorsDestroyed() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ScopedTargetService.class).work());
        }
        System.out.println("scoped destroy: postConstructed=" + PairedInterceptor.POST_CONSTRUCTED
            + " invoked=" + PairedInterceptor.INVOKED + " destroyed=" + PairedInterceptor.DESTROYED);
        assertEquals(PairedInterceptor.POST_CONSTRUCTED, PairedInterceptor.DESTROYED,
            "every post-constructing instance destroyed");
    }

    @Test
    void proxyAroundAnExistingTarget() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            // the target is created by another resolution first
            context.getProxyTargetBean(EagerTargetService.class, null);
            assertEquals("done", context.getBean(EagerTargetService.class).work());
            assertOneInstance("existing target");
        }
        assertDestroyedOnce("existing target");
    }

    @Test
    void proxyAroundATargetCreatedOnAnotherThread() throws Exception {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            Thread thread = new Thread(() -> context.getProxyTargetBean(EagerTargetService.class, null));
            thread.start();
            thread.join();
            assertEquals("done", context.getBean(EagerTargetService.class).work());
            assertOneInstance("target from another thread");
        }
        assertDestroyedOnce("target from another thread");
    }

    private static void assertOneInstance(String scenario) {
        System.out.println(scenario + ": postConstructed=" + PairedInterceptor.POST_CONSTRUCTED
            + " invoked=" + PairedInterceptor.INVOKED);
        assertEquals(1, PairedInterceptor.POST_CONSTRUCTED.size(), scenario + ": post-constructed instances");
        assertEquals(1, PairedInterceptor.INVOKED.size(), scenario + ": invoked instances");
        PairedInterceptor invoked = PairedInterceptor.INVOKED.get(0);
        assertSame(PairedInterceptor.POST_CONSTRUCTED.get(0), invoked, scenario + ": same instance");
        assertNotNull(invoked.created(), scenario + ": state kept from post-construct");
    }

    private static void assertDestroyedOnce(String scenario) {
        System.out.println(scenario + ": destroyed=" + PairedInterceptor.DESTROYED);
        assertEquals(List.of(PairedInterceptor.POST_CONSTRUCTED.get(0)), PairedInterceptor.DESTROYED,
            scenario + ": destroyed instances");
    }
}
