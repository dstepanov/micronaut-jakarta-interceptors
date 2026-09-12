
package io.micronaut.interceptor.test.destruction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What was destroyed, in the order it happened.
 */
public final class Destruction {

    public static final List<String> CALLS = new CopyOnWriteArrayList<>();

    private Destruction() {
    }
}
