package io.micronaut.interceptor.test.reflection;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interceptor method is invoked through the executable method Micronaut generated for it at compilation time.
 * The stack of an interceptor method shows how that executable method reached it.
 */
class NoReflectionTest {

    /**
     * An interceptor method that is not private is called directly by the executable method: between the method
     * and the proxy that started the invocation there is nothing of the reflection API.
     */
    @Test
    void invokesTheInterceptorMethodWithoutReflection() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(MeasuredService.class).work());
        }

        List<String> between = betweenInterceptorMethodAndCaller(MeasuringInterceptor.frames,
            MeasuringInterceptor.class.getName() + ".measure",
            "invokesTheInterceptorMethodWithoutReflection");
        List<String> reflective = between.stream().filter(Frames::isReflective).toList();
        assertEquals(List.of(), reflective, "the interception must not go through reflection, but did: " + between);
    }

    /**
     * Generated code cannot call a private method of another class, so the executable method of a private
     * interceptor method calls it through {@code java.lang.reflect.Method}, which the {@code @ReflectiveAccess} the
     * processor declares on the method permits. It is the one reflective call of the interception itself.
     *
     * <p>Not run inside a native image, whose reflective calls go through frames of the image's own. The
     * private around-timeout method of the {@code timeout} tests is invoked there, which is what shows the metadata
     * is in place.</p>
     */
    @Test
    @DisabledInNativeImage
    void invokesAPrivateInterceptorMethodReflectively() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(PrivatelyMeasuredService.class).work());
        }

        List<String> between = betweenInterceptorMethodAndCaller(PrivateMeasuringInterceptor.frames,
            PrivateMeasuringInterceptor.class.getName() + ".measure",
            "invokesAPrivateInterceptorMethodReflectively");
        assertTrue(between.contains("java.lang.reflect.Method.invoke"),
            "a private interceptor method is invoked reflectively: " + between);
        assertTrue(between.contains("io.micronaut.core.reflect.ReflectionUtils.invokeMethod"),
            "a private interceptor method is invoked reflectively: " + between);
    }

    private static List<String> betweenInterceptorMethodAndCaller(List<String> frames, String interceptorFrame,
                                                                  String testMethod) {
        int interceptorMethod = frames.indexOf(interceptorFrame);
        int callSite = frames.indexOf(NoReflectionTest.class.getName() + "." + testMethod);
        assertTrue(interceptorMethod >= 0, "the interceptor method is on the stack: " + frames);
        assertTrue(callSite > interceptorMethod, "the caller is on the stack: " + frames);
        return frames.subList(interceptorMethod, callSite);
    }
}
