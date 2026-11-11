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
import java.util.concurrent.atomic.LongAdder;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The beans many threads create at once, racing to build the same chains, are each constructed and invoked through
 * the whole of their chains.
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
        static final LongAdder INVOKED = new LongAdder();
        static final LongAdder CONSTRUCTED = new LongAdder();

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            INVOKED.increment();
            return context.proceed();
        }

        @AroundConstruct
        public Object construct(InvocationContext context) throws Exception {
            CONSTRUCTED.increment();
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
    void concurrentlyCreatedPrototypesRunTheirWholeChains() throws Exception {
        int threads = 8;
        int beansPerThread = 50;
        try (ApplicationContext context = ApplicationContext.run()) {
            RacedInterceptor.INVOKED.reset();
            RacedInterceptor.CONSTRUCTED.reset();
            createConcurrently(context, threads, beansPerThread);

            assertEquals(threads * beansPerThread, RacedInterceptor.CONSTRUCTED.sum());
            assertEquals(2L * threads * beansPerThread, RacedInterceptor.INVOKED.sum());
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
}
