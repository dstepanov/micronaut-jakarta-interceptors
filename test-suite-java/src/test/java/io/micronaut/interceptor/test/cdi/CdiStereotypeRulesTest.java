package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdiStereotypeRulesTest {

    @Test
    void aBindingCarriedByAnInheritedStereotypeOfASuperclassReachesTheBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(StereotypeInheritingService.class).stereotyped();

            assertEquals(List.of("secure", "stereotyped"), List.copyOf(Calls.RECORDED));
        }
    }

}
