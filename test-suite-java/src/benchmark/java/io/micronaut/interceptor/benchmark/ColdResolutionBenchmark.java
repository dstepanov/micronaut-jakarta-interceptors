package io.micronaut.interceptor.benchmark;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.benchmark.generated.GeneratedBeans;
import io.micronaut.interceptor.benchmark.generated.Invocations;
import io.micronaut.interceptor.internal.runtime.InterceptorChainResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Times resolving every chain of a large application for the first time, and creating its beans again once the chains
 * are known.
 *
 * <p>Each iteration starts a context of its own, so that the resolver, a singleton of the context, starts empty, and
 * creates one bean of every generated class, which resolves every chain of it. The iterations run in one JVM after a
 * warm-up, so that what is measured is the work of the resolver rather than the compiling of the code doing it. The
 * creations repeated afterwards find every chain resolved already.</p>
 */
class ColdResolutionBenchmark {

    private static final int WARMUP = Integer.getInteger("benchmark.warmup", 20);
    private static final int ITERATIONS = Integer.getInteger("benchmark.iterations", 40);
    private static final int ROUNDS = Integer.getInteger("benchmark.rounds", 200);
    private static final String LABEL = System.getProperty("benchmark.label", "unlabelled");

    @Test
    void coldResolutionAndRepeatedCreation() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            // the chains measured are the ones that intercept: every method is bound to five to ten interceptors
            Object bean = context.getBean(GeneratedBeans.ALL.get(0));
            GeneratedBeans.ALL.get(0).getMethod("m0", int.class).invoke(bean, 1);
            if (Invocations.COUNT.sum() < 5) {
                throw new AssertionError("intercepted by " + Invocations.COUNT.sum());
            }
            System.out.println("BENCHMARK sanity: m0 of Bean0 intercepted by " + Invocations.COUNT.sum());
        }
        long[] start = new long[ITERATIONS];
        long[] firstResolution = new long[ITERATIONS];
        long[] cold = new long[ITERATIONS];
        long firstJvmIteration = -1;
        int chains = 0;
        for (int i = -WARMUP; i < ITERATIONS; i++) {
            System.gc();
            long t0 = System.nanoTime();
            try (ApplicationContext context = ApplicationContext.run()) {
                long t1 = System.nanoTime();
                createAll(context);
                long t2 = System.nanoTime();
                chains = context.getBean(InterceptorChainResolver.class).cachedChains();
                if (i == -WARMUP) {
                    firstJvmIteration = t2 - t0;
                }
                if (i >= 0) {
                    start[i] = t1 - t0;
                    firstResolution[i] = t2 - t1;
                    cold[i] = t2 - t0;
                }
            }
        }

        long[] repeated = new long[ROUNDS];
        try (ApplicationContext context = ApplicationContext.run()) {
            for (int r = -ROUNDS; r < ROUNDS; r++) {
                long t0 = System.nanoTime();
                createAll(context);
                long t1 = System.nanoTime();
                if (r >= 0) {
                    repeated[r] = t1 - t0;
                }
            }
        }

        String report = String.format(Locale.ROOT,
            "%s beans=%d chains=%d | start median %.2f ms | first resolution median %.2f ms (p25 %.2f, p75 %.2f) "
                + "| cold total median %.2f ms (p25 %.2f, p75 %.2f) | repeated creation of all median %.1f us "
                + "(p25 %.1f, p75 %.1f) | first JVM iteration %.1f ms",
            LABEL, GeneratedBeans.ALL.size(), chains,
            ms(percentile(start, 50)),
            ms(percentile(firstResolution, 50)), ms(percentile(firstResolution, 25)), ms(percentile(firstResolution, 75)),
            ms(percentile(cold, 50)), ms(percentile(cold, 25)), ms(percentile(cold, 75)),
            us(percentile(repeated, 50)), us(percentile(repeated, 25)), us(percentile(repeated, 75)),
            ms(firstJvmIteration));
        System.out.println("BENCHMARK " + report);
        String out = System.getProperty("benchmark.out");
        if (out != null) {
            Files.writeString(Path.of(out), report + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private static void createAll(ApplicationContext context) {
        List<Object> created = new ArrayList<>(GeneratedBeans.ALL.size());
        for (Class<?> type : GeneratedBeans.ALL) {
            created.add(context.getBean(type));
        }
        if (created.size() != GeneratedBeans.ALL.size()) {
            throw new AssertionError();
        }
    }

    private static long percentile(long[] values, int percentile) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[Math.min(sorted.length - 1, sorted.length * percentile / 100)];
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static double us(long nanos) {
        return nanos / 1_000.0;
    }
}
