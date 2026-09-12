/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.interceptor.test.access;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A protected business method is a non-private, non-static method, which the specification intercepts as it does a
 * public one; the reference implementation intercepts it too. Micronaut applies the advice a class declares only to
 * its public and package private methods, so the interception of a protected method is declared on the method
 * itself.
 */
class ProtectedBusinessMethodTest {

    @Test
    void aProtectedMethodOfAClassBoundAtClassLevelIsIntercepted() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            WatchedService service = context.getBean(WatchedService.class);

            assertEquals("prot", service.prot());
            assertEquals(List.of("watched prot", "prot"), Calls.RECORDED);
        }
    }

    @Test
    void andSoIsOneCalledFromAnotherMethodOfTheSameBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            WatchedService service = context.getBean(WatchedService.class);

            assertEquals("prot", service.callsProtected());
            // the proxy is the bean, so the call on this goes through the method the proxy overrides
            assertEquals(List.of("watched callsProtected", "callsProtected", "watched prot", "prot"),
                Calls.RECORDED);
        }
    }

    @Test
    void asArePublicAndPackagePrivateMethods() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            WatchedService service = context.getBean(WatchedService.class);

            assertEquals("pub", service.pub());
            assertEquals("pkg", service.pkg());
            assertEquals(List.of("watched pub", "pub", "watched pkg", "pkg"), Calls.RECORDED);
        }
    }

    @Test
    void aProtectedMethodBoundByItsOwnBindingIsInterceptedAndNothingElseIs() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            MethodWatchedService service = context.getBean(MethodWatchedService.class);

            assertEquals("prot", service.prot());
            assertEquals("unwatched", service.unwatched());
            assertEquals(List.of("watched prot", "prot", "unwatched"), Calls.RECORDED);
        }
    }

    @Test
    void aProtectedMethodIsInterposedOnByTheInterceptorClassesItsClassNames() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            NamingService service = context.getBean(NamingService.class);

            assertEquals("prot", service.prot());
            assertEquals(List.of("watched prot", "prot"), Calls.RECORDED);
        }
    }

    @Test
    void unlessItExcludesThem() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            NamingService service = context.getBean(NamingService.class);

            assertEquals("excluding", service.excluding());
            assertEquals(List.of("excluding"), Calls.RECORDED);
        }
    }
}
