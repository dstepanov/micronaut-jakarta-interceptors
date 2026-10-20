package io.micronaut.interceptor.test.eager;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3: the interceptor instances of an object proxied as a subclass are created when the object is created, bound
 * on the class so that its construction is intercepted as well. Micronaut hands the advice of the subclass the bean
 * it is created for as it resolves the arguments of the constructor, so the instances exist before the constructor
 * runs, as the reference implementation has them; a bean created through {@code BeanContext.createBean} is handed
 * to its advice as well, since Micronaut begins a creation for it as it does for a lookup.
 */
class SubclassCreationTest {

    static final List<String> STEPS = Collections.synchronizedList(new ArrayList<>());

    @Test
    void aBeanLookedUpCreatesItsInterceptorsBeforeItsConstructorRuns() {
        STEPS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ClassLevelPlain.class).work());
            assertEquals(List.of("interceptor created", "object constructed"), STEPS);
        }
    }

    @Test
    void aBeanCreatedOnDemandCreatesItsInterceptorsWithTheObject() {
        STEPS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            ClassLevelPlain bean = context.createBean(ClassLevelPlain.class);
            assertEquals(List.of("interceptor created"), STEPS.stream().filter("interceptor created"::equals).toList(),
                "created with the object, before anything is invoked on it: " + STEPS);
            assertEquals("done", bean.work());
            assertEquals(1, STEPS.stream().filter("interceptor created"::equals).count(), "one instance: " + STEPS);
        }
    }
}
