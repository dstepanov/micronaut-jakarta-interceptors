package io.micronaut.interceptor.test.identity;

import io.micronaut.aop.Intercepted;
import io.micronaut.aop.Interceptor;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.interceptor.runtime.JakartaInterceptorAdvice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The advice of an intercepted bean is one object, however much that bean declares.
 *
 * <p>Handing the rest of the chain over to Micronaut is {@code InvocationContext.proceed(advice)}, and Micronaut
 * resumes after the first interceptor of the invocation that is the same object as the one handed to it. Were the
 * same advice object in the array of an invocation twice, resuming from its second occurrence would return to that
 * occurrence and never reach what comes after. Micronaut builds that array from the interceptor registrations of the
 * proxy, one interceptor apiece, and the advice is a prototype, so each bean has one of its own and it is there
 * once; this holds that to be so.</p>
 */
class AdviceIdentityTest {

    @Test
    void bindsOneAdviceToABeanThatDeclaresEveryKindOfInterception() {
        try (ApplicationContext context = ApplicationContext.run()) {
            HeavilyBoundService service = context.getBean(HeavilyBoundService.class);
            assertEquals("done", service.work());
            assertEquals("again", service.boundAgain());

            Intercepted intercepted = assertInstanceOf(Intercepted.class, service);
            int advices = 0;
            for (BeanRegistration<Interceptor<?, ?>> registration : intercepted.$interceptorRegistrations()) {
                if (registration.getBean() instanceof JakartaInterceptorAdvice) {
                    advices++;
                }
            }
            assertEquals(1, advices, "the bean is bound to one advice, whose chain resumes by its identity");
        }
    }
}
