package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The typed binding accessors Jakarta Interceptors 2.2 added beside {@code getInterceptorBindings()}. The
 * specification gives them a default that filters the whole set; this module answers them without building the
 * rest, so what they return has to be held to the same answers.
 */
class TypedBindingTest {

    @Test
    void readsOneBindingAndARepeatedOneByType() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.RECORDED.clear();

            assertEquals("done", context.getBean(TypedBindingService.class).work());

            // UsersRegionInterceptor is bound by the @Cached(region = "users") the element also carries, and
            // runs after this one, the two sharing a priority and being ordered by their class name
            assertEquals(
                List.of("cached=users",
                        "labelled=[one, two]",
                        "tagged=null",
                        "taggedAll=[]",
                        "full=[Cached, Labelled(one), Labelled(two), TypedRead]",
                        "users region"),
                List.copyOf(Calls.RECORDED));
        }
    }

    /**
     * The binding of one type a business method reports is built once and kept by the method, as the whole set is:
     * an interceptor asking for its binding on every invocation is handed the same annotation each time.
     */
    @Test
    void keepsTheBindingOfOneTypeAcrossInvocations() {
        try (ApplicationContext context = ApplicationContext.run()) {
            TypedBindingInterceptor.HANDED.clear();
            TypedBindingService service = context.getBean(TypedBindingService.class);

            service.work();
            service.work();

            assertEquals(2, TypedBindingInterceptor.HANDED.size());
            assertSame(TypedBindingInterceptor.HANDED.get(0), TypedBindingInterceptor.HANDED.get(1));
        }
    }

    /**
     * The typed accessors from every kind of context. A construction reads its bindings from the metadata of the
     * constructor and a lifecycle event from the class, so what is true of a method says nothing of them. The
     * singular accessor is read before the whole set is built and after, which are separate paths through the
     * context, and on a repeated binding it has to answer with one of the values the plural accessor gives.
     */
    @Test
    void readsARepeatedBindingByTypeFromEveryKindOfContext() {
        try (ApplicationContext context = ApplicationContext.run()) {
            TypedLifecycleCalls.RECORDED.clear();

            context.createBean(TypedLifecycleService.class).work();

            List<String> recorded = List.copyOf(TypedLifecycleCalls.RECORDED);
            assertEquals(3, recorded.size(), "one record for each kind: " + recorded);
            for (String kind : List.of("aroundConstruct", "postConstruct", "aroundInvoke")) {
                String line = recorded.stream().filter(it -> it.startsWith(kind + " ")).findFirst()
                    .orElseThrow(() -> new AssertionError("nothing recorded for " + kind + ": " + recorded));
                assertTrue(line.contains("plural=[one, two]"), "both values of the repeated binding: " + line);
                assertTrue(line.contains("cached=orders"), "the ordinary binding beside it: " + line);
                String cold = line.replaceAll(".* cold=(\\S+).*", "$1");
                String warm = line.replaceAll(".* warm=(\\S+).*", "$1");
                assertTrue(List.of("one", "two").contains(cold),
                    "the singular accessor before the set is built answers with one of the values: " + line);
                assertEquals(cold, warm, "and with the same one after it is built: " + line);
            }
        }
    }
}