package io.micronaut.interceptor.test;

import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * What the scheduled fixtures and their interceptors observed, kept by the context they run in.
 *
 * <p>A singleton of the context rather than static state: a schedule keeps running for as long as its context does,
 * and what one test's context observed is gone with it, so a later test cannot pass on an execution of an earlier
 * one.</p>
 */
@Singleton
public class ScheduledRecords {

    private final Map<String, CountDownLatch> runs = new ConcurrentHashMap<>();
    private final Map<String, List<String>> logs = new ConcurrentHashMap<>();
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();

    /**
     * Records that a schedule ran.
     *
     * @param schedule The schedule
     */
    public void ran(String schedule) {
        latch(schedule).countDown();
    }

    /**
     * Waits for a schedule of this context to run.
     *
     * @param schedule The schedule
     * @return Whether it ran within five seconds
     * @throws InterruptedException If the wait is interrupted
     */
    public boolean awaitRun(String schedule) throws InterruptedException {
        return latch(schedule).await(5, TimeUnit.SECONDS);
    }

    /**
     * Adds an entry to a log.
     *
     * @param log   The log
     * @param entry The entry
     */
    public void record(String log, String entry) {
        logOf(log).add(entry);
    }

    /**
     * @param log The log
     * @return What the log holds so far
     */
    public List<String> recorded(String log) {
        return List.copyOf(logOf(log));
    }

    /**
     * Records the current thread under a key, unless one was recorded there already.
     *
     * @param key The key
     * @return Whether the current thread is the one recorded
     */
    public boolean firstThread(String key) {
        return threads.putIfAbsent(key, Thread.currentThread()) == null;
    }

    /**
     * @param key The key
     * @return The thread first recorded under the key, or {@code null}
     */
    public @Nullable Thread thread(String key) {
        return threads.get(key);
    }

    private CountDownLatch latch(String schedule) {
        return runs.computeIfAbsent(schedule, name -> new CountDownLatch(1));
    }

    private List<String> logOf(String log) {
        return logs.computeIfAbsent(log, name -> new CopyOnWriteArrayList<>());
    }
}
