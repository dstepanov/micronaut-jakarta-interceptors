package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 3.4.2 c) of the specification leaves array-valued binding members to an extension. They are compared by
 * their contents here, so that a binding with an array member behaves as one would expect of any other member.
 */
class ArrayBindingMemberTest {

    @Test
    void comparesAnArrayValuedMemberByItsContents() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(RolesService.class).matching();

            assertEquals(List.of("admin roles"), Calls.RECORDED);
        }
    }

    @Test
    void doesNotBindWhenTheContentsDiffer() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(RolesService.class).notMatching();

            assertEquals(List.of(), Calls.RECORDED);
        }
    }
}
