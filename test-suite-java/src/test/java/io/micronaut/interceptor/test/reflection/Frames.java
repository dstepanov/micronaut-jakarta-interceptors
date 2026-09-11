package io.micronaut.interceptor.test.reflection;

import java.util.List;
import java.util.Set;

final class Frames {

    private Frames() {
    }

    /**
     * The frames of the calling thread, reflective ones included: a stack walker leaves out the frames of
     * {@code java.lang.reflect.Method.invoke} and what it calls into unless it is asked to show them, and without
     * them a reflective call would look like a direct one.
     */
    static List<String> current() {
        return StackWalker.getInstance(Set.of(StackWalker.Option.SHOW_REFLECT_FRAMES))
            .walk(stream -> stream
                .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                .toList());
    }

    static boolean isReflective(String frame) {
        return frame.startsWith("java.lang.reflect.")
            || frame.startsWith("jdk.internal.reflect.")
            || frame.startsWith("java.lang.invoke.")
            || frame.startsWith("io.micronaut.core.reflect.");
    }
}
