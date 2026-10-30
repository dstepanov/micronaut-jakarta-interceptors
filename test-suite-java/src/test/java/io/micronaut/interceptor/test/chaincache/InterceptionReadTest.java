package io.micronaut.interceptor.test.chaincache;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.internal.runtime.InterceptorChainResolver;
import io.micronaut.interceptor.test.edge.PrototypeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reading the interception of an element into what its chain is built from brings every binding of it into the
 * shape bindings are compared in, which is the costly part of resolving a chain. It is done once for each
 * interception an application records, not again for every bean created and every invocation that looks the chain up.
 */
class InterceptionReadTest {

    @Test
    void readsTheInterceptionOfAnElementOnceHoweverManyBeansPassThroughIt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            context.createBean(PrototypeService.class).work();
            long read = resolver.readInterceptions();

            for (int i = 0; i < 200; i++) {
                context.createBean(PrototypeService.class).work();
            }

            assertEquals(read, resolver.readInterceptions(),
                "the interception of an element is read once, however many beans of it are created");
        }
    }
}
