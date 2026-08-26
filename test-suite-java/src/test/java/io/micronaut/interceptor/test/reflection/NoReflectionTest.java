package io.micronaut.interceptor.test.reflection;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interceptor method is invoked through the executable method Micronaut generated for it at compilation time,
 * never reflectively. The stack of an interceptor method shows it: between the method and the proxy that started
 * the invocation there is nothing of the reflection API.
 */
class NoReflectionTest {

    @Test
    void invokesTheInterceptorMethodWithoutReflection() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(MeasuredService.class).work());
        }

        List<String> frames = MeasuringInterceptor.frames;
        int interceptorMethod = frames.indexOf(MeasuringInterceptor.class.getName() + ".measure");
        int callSite = frames.indexOf(NoReflectionTest.class.getName() + ".invokesTheInterceptorMethodWithoutReflection");
        assertTrue(interceptorMethod >= 0, "the interceptor method is on the stack: " + frames);
        assertTrue(callSite > interceptorMethod, "the caller is on the stack: " + frames);

        List<String> between = frames.subList(interceptorMethod, callSite);
        List<String> reflective = between.stream().filter(NoReflectionTest::isReflective).toList();
        assertEquals(List.of(), reflective, "the interception must not go through reflection, but did: " + between);
    }

    private static boolean isReflective(String frame) {
        return frame.startsWith("java.lang.reflect.")
            || frame.startsWith("jdk.internal.reflect.")
            || frame.startsWith("java.lang.invoke.");
    }
}
