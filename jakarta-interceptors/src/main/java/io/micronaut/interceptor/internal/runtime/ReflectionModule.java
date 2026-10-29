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
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.core.annotation.Internal;
import io.micronaut.reflection.ReflectionExecutables;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Whether the reflection module is on the classpath, found out the first time an accessor that needs it is
 * called - a class is initialized on its first use, so an application whose interceptors call none of those
 * accessors never looks for the module at all.
 *
 * <p>It stands beside {@link PlatformReflection}, which links to that module and fails to load without it, and is
 * what is asked before that class is used.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ReflectionModule {

    /**
     * What loading a class of the module failed with, or {@code null} where the module is there. It is the
     * cause of every failure that reports the module missing, which is the same for each of them.
     */
    @SuppressWarnings("StaticAssignmentOfThrowable")
    static final @Nullable NoClassDefFoundError MISSING = find();

    private ReflectionModule() {
    }

    /**
     * Makes sure the reflection module is on the classpath before {@link PlatformReflection} is used to answer an
     * accessor.
     *
     * <p>{@link PlatformReflection} links to that module and fails to load without it. Rather than recognising that
     * failure among the others an accessor may run into, the module is looked for once, here, and an accessor that
     * needs it asks first. A class of the application that is missing - one a binding annotation refers to, for
     * instance - therefore fails as it is, naming that class, and is never taken for the module.</p>
     *
     * @param accessor The accessor of {@code InvocationContext} that was called
     * @throws UnsupportedOperationException naming the accessor and the dependency to add, where the module is absent
     */
    static void require(String accessor) {
        NoClassDefFoundError missing = MISSING;
        if (missing != null) {
            throw unavailable(accessor, missing);
        }
    }

    /**
     * The failure of an accessor that returns an object of the reflection of the platform, in an application
     * without the module that answers it.
     *
     * @param accessor The accessor of {@code InvocationContext} that was called
     * @param cause    What looking for the module failed with
     * @return The exception to throw
     */
    static UnsupportedOperationException unavailable(String accessor, NoClassDefFoundError cause) {
        return new UnsupportedOperationException("InvocationContext." + accessor + " returns an object of the Java"
            + " reflection API, which needs io.micronaut:micronaut-reflection on the classpath (or"
            + " io.micronaut.cdi:micronaut-cdi-reflection, which brings it, when the interceptors are used through"
            + " Micronaut CDI). Add that dependency, or read the interception through MicronautInvocationContext,"
            + " which reflects on nothing.", cause);
    }

    private static @Nullable NoClassDefFoundError find() {
        try {
            // a class literal is resolved, and so loaded, where it is evaluated
            Objects.requireNonNull(ReflectionExecutables.class);
            return null;
        } catch (NoClassDefFoundError e) {
            return e;
        }
    }
}
