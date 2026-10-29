package io.micronaut.interceptor.test.graalvm;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;

/**
 * A factory whose only binding is the one on the member producing its bean.
 */
@Factory
public class ProducingFactory {
    @Bean
    @Produced(origin = @Origin("factory"))
    ProducedService service() {
        return new ProducedService();
    }
}
