package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 3.4.2 c) of the specification, for every primitive array type. ArrayBindingMemberTest holds a
 * {@code String[]} member to being compared by its contents, which is the branch for an array of objects; each
 * primitive array type is normalised by a branch of its own, and none of them was reached.
 */
class PrimitiveArrayBindingTest {

    @Test
    void comparesEveryPrimitiveArrayTypeByItsContents() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            PrimitiveArraysService service = context.getBean(PrimitiveArraysService.class);

            service.matching();
            service.differentBools();
            service.differentBytes();
            service.differentShorts();
            service.differentChars();
            service.differentInts();
            service.differentLongs();
            service.differentFloats();
            service.differentDoubles();

            assertEquals(List.of("matching"), List.copyOf(Calls.RECORDED),
                "equal contents bind in every member, and different contents in any one member do not");
        }
    }
}
