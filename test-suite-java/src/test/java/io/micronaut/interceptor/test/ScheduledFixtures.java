package io.micronaut.interceptor.test;

import io.micronaut.context.ApplicationContext;

import java.util.Map;

/**
 * The {@code @Scheduled} beans of this source set require {@link #ENABLED}, and only the tests of scheduling start
 * a context with it.
 *
 * <p>The scheduler creates the beans it runs on a thread of its own a few milliseconds after the context starts,
 * and resolves their chains whenever that lands. In every other context that is work no test started racing the
 * work a test measures, so the beans are left out of them.</p>
 */
public final class ScheduledFixtures {

    /**
     * The property the {@code @Scheduled} beans of this source set require.
     */
    public static final String ENABLED = "test.scheduled-fixtures.enabled";

    private ScheduledFixtures() {
    }

    /**
     * @return a started context holding the {@code @Scheduled} beans of this source set
     */
    public static ApplicationContext run() {
        return ApplicationContext.run(Map.of(ENABLED, "true"));
    }
}
