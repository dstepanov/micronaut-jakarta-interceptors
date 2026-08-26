package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProducerInterceptionTest {

    @Test
    void interceptsABeanProducedByAFactoryMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();

            assertEquals("report", context.getBean(Reporter.class).report());
            assertEquals(List.of("monitored", "report"), Calls.RECORDED);
        }
    }
}
