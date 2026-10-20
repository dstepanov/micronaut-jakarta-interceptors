package io.micronaut.interceptor.internal.runtime;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Prototype;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chains of a bean many threads create at once are each resolved at most once for every thread racing to build
 * it, and never again once they are built, however many more of the beans are created.
 */
class ConcurrentChainResolutionTest {

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, CONSTRUCTOR, METHOD})
    public @interface Raced {
    }

    @Interceptor
    @Raced
    public static class RacedInterceptor {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }

        @AroundConstruct
        public Object construct(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Prototype
    @Raced
    public static class RacedPrototype {
        public String work() {
            return "worked";
        }

        public String rest() {
            return "rested";
        }
    }

    @Test
    void theChainsOfConcurrentlyCreatedPrototypesAreNotResolvedAgain() throws Exception {
        long ofOneBean;
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            long before = settled(resolver);
            RacedPrototype prototype = context.getBean(RacedPrototype.class);
            prototype.work();
            prototype.rest();
            ofOneBean = settled(resolver) - before;
            assertTrue(ofOneBean > 0, "the first bean resolves its chains");
        }

        int threads = 8;
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            long before = settled(resolver);
            createConcurrently(context, threads, 50);
            long created = settled(resolver) - before;
            assertTrue(created >= ofOneBean && created <= ofOneBean * threads,
                "at most one resolution for each thread racing to build a chain, " + created + " where one bean made "
                    + ofOneBean);

            long settledCount = settled(resolver);
            createConcurrently(context, threads, 200);
            assertEquals(settledCount, settled(resolver), "built chains are looked up, not resolved again");
        }
    }

    private static void createConcurrently(ApplicationContext context, int threads, int beansPerThread)
        throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    start.await();
                    for (int bean = 0; bean < beansPerThread; bean++) {
                        RacedPrototype prototype = context.getBean(RacedPrototype.class);
                        assertEquals("worked", prototype.work());
                        assertEquals("rested", prototype.rest());
                    }
                    return null;
                });
            }
            List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * The count once the beans the scheduler creates on a thread of its own have been resolved, which is the count
     * holding still across consecutive reads.
     */
    private static long settled(InterceptorChainResolver resolver) {
        long previous = -1;
        int stable = 0;
        for (int attempt = 0; attempt < 100; attempt++) {
            long current = resolver.readInterceptions();
            stable = current == previous ? stable + 1 : 0;
            if (stable == 5) {
                return current;
            }
            previous = current;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the count to settle", e);
            }
        }
        throw new AssertionError("The count never settled, last seen " + previous);
    }
}
