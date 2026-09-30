package io.micronaut.interceptor.test.noreflection;

import io.micronaut.aop.InterceptorKind;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An application with {@code micronaut-jakarta-interceptors} and without {@code micronaut-reflection}.
 *
 * <p>Everything is intercepted as it is with the module. What differs are the accessors of
 * {@code InvocationContext} that return an object of the Java reflection API: the module is what answers them, and
 * without it they fail, saying which dependency to add.</p>
 */
class WithoutReflectionModuleTest {

    @BeforeEach
    void clear() {
        Probe.clear();
    }

    /**
     * What the rest of this class shows holds only while the module is absent.
     */
    @Test
    void theReflectionModuleIsAbsent() {
        assertNull(WithoutReflectionModuleTest.class.getClassLoader()
            .getResource("io/micronaut/reflection/ReflectionExecutables.class"));
    }

    /**
     * The processor declares for reflection only what can be reflected on. The private interceptor method is
     * called reflectively, so it is declared. The constructor of the intercepted class and the proxies of its
     * binding annotations are what {@code getConstructor()} and the binding accessors return, which cannot be
     * called here, so the intercepted class declares nothing.
     *
     * <p>Not run inside a native image, which carries the configurers themselves rather than the entries they
     * are found by.</p>
     */
    @Test
    @DisabledInNativeImage
    void declaresNoReachabilityMetadataForTheAccessorsThatNeedTheModule() {
        String configurers = "META-INF/micronaut/io.micronaut.core.graal.GraalReflectionConfigurer/";
        ClassLoader loader = WithoutReflectionModuleTest.class.getClassLoader();
        String packageName = WithoutReflectionModuleTest.class.getPackageName();
        assertNotNull(loader.getResource(configurers + packageName + ".$AuditingInterceptor$ReflectConfig"));
        assertNull(loader.getResource(configurers + packageName + ".$AuditedService$ReflectConfig"));
    }

    /**
     * Construction, the lifecycle callbacks and a business method are intercepted, the business method by a private
     * interceptor method, and the interceptor replaces the argument.
     */
    @Test
    void interceptsEveryKindOfInterception() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done replaced", context.getBean(AuditedService.class).work("task"));
        }

        assertEquals(
            List.of("aroundConstruct", "constructor", "postConstruct", "init", "aroundInvoke", "work replaced",
                "preDestroy", "close"),
            Probe.CALLS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "getMethod()",
        "postConstruct getMethod()",
        "getConstructor()",
        "getInterceptorBindings()",
        "getInterceptorBinding(Class)",
        "getInterceptorBindings(Class)"
    })
    void anAccessorThatReturnsAReflectionObjectNamesTheModuleItNeeds(String accessor) {
        try (ApplicationContext context = ApplicationContext.run()) {
            AuditedService service = context.getBean(AuditedService.class);
            service.work("first");
            assertRequiresTheModule(accessor);
            if (!accessor.startsWith("postConstruct") && !accessor.equals("getConstructor()")) {
                // the failure is the same however many times the accessor is called
                Probe.FAILURES.clear();
                service.work("second");
                assertRequiresTheModule(accessor);
            }
        }
    }

    /**
     * Whether an annotation is a binding of the element is read from the annotation metadata, so asking for one
     * that is not builds nothing, and answers without the module.
     */
    @Test
    void anAnnotationThatIsNoBindingIsAnsweredWithoutTheModule() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(AuditedService.class).work("task");
        }

        assertFalse(Probe.FAILURES.containsKey("getInterceptorBinding(Class) of no binding"));
        assertNull(Probe.ANSWERS.get("getInterceptorBinding(Class) of no binding"));
        assertEquals(Set.of(), Probe.ANSWERS.get("getInterceptorBindings(Class) of no binding"));
    }

    @Test
    void theMicronautInvocationContextDescribesTheInterceptionWithoutTheModule() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(AuditedService.class).work("task");
        }

        assertEquals(InterceptorKind.AROUND_CONSTRUCT, Probe.ANSWERS.get("constructor kind"));
        assertEquals(AuditedService.class, Probe.ANSWERS.get("constructor type"));
        assertEquals(0, Probe.ANSWERS.get("constructor arguments"));
        assertEquals(true, Probe.ANSWERS.get("constructor binding"));

        assertEquals(InterceptorKind.POST_CONSTRUCT, Probe.ANSWERS.get("postConstruct kind"));
        assertEquals("init", Probe.ANSWERS.get("postConstruct method"));

        assertEquals(InterceptorKind.AROUND, Probe.ANSWERS.get("method kind"));
        assertEquals("work", Probe.ANSWERS.get("method"));
        assertEquals(AuditedService.class, Probe.ANSWERS.get("method declaring type"));
        assertEquals(true, Probe.ANSWERS.get("method binding"));
        assertEquals("task", Probe.ANSWERS.get("parameter"));
    }

    private static void assertRequiresTheModule(String accessor) {
        Throwable failure = Probe.FAILURES.get(accessor);
        assertNotNull(failure, accessor + " answered " + Probe.ANSWERS.get(accessor) + " without the reflection module");
        assertInstanceOf(UnsupportedOperationException.class, failure);
        String message = failure.getMessage();
        String name = accessor.substring(accessor.indexOf("get"));
        assertTrue(message.contains("InvocationContext." + name), message);
        assertTrue(message.contains("io.micronaut:micronaut-reflection"), message);
        assertTrue(message.contains("micronaut-cdi-reflection"), message);
        assertInstanceOf(NoClassDefFoundError.class, failure.getCause());
    }
}
