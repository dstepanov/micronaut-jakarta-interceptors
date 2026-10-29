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

import io.micronaut.core.annotation.AnnotationBuilder;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.MethodReference;
import io.micronaut.reflection.ReflectionAnnotations;
import io.micronaut.reflection.ReflectionExecutables;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * What of this module hands back an object of the reflection of the platform: the {@link Method} of an intercepted
 * element, and the instances of its binding annotations. The constructor of the platform is looked up by Micronaut
 * Core itself, so it does not come here.
 *
 * <p>The instance of a binding whose builder was generated with {@code @RegisterAnnotations} is built by that
 * builder. Everything else is answered by {@code io.micronaut:micronaut-reflection}, which this module compiles
 * against and does not depend on. Only {@link Linked} links to that module, so this class loads without it; an
 * accessor that needs the module looks for it first, once for the whole application, the first time one of them is
 * called, and throws an {@link UnsupportedOperationException} that names the module where it is not. An application
 * whose interceptors call none of those accessors never looks for the module.</p>
 *
 * <p>Looking for the module rather than recognising the failure of whatever happened to use it means a class of the
 * application that is missing - one a binding annotation refers to, for instance - fails as it is, naming that
 * class, and is never taken for the module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class PlatformReflection {

    private PlatformReflection() {
    }

    /**
     * Looks up the method of the platform an intercepted method stands for.
     *
     * @param method The intercepted method, one that stands for a method of the platform. A lifecycle event of a
     *               class that declares no callback stands for none, which
     *               {@link MethodReference#hasTargetMethod()} tells without this class
     * @return The method of the platform
     */
    static Method method(MethodReference<?, ?> method) {
        requireModule("getMethod()");
        return Linked.method(method);
    }

    /**
     * Builds an instance of a binding annotation of a known type from the value Micronaut recorded for it.
     *
     * @param annotationType The annotation type
     * @param value          A value of the annotation of that type, which is what it is read as
     * @param accessor       The accessor of {@code InvocationContext} that was called
     * @param <A>            The annotation type
     * @return The instance
     */
    @SuppressWarnings("unchecked")
    static <A extends Annotation> A annotation(Class<A> annotationType, AnnotationValue<?> value, String accessor) {
        Optional<AnnotationBuilder<A>> builder = AnnotationBuilder.find(annotationType);
        if (builder.isPresent()) {
            return builder.get().build(value);
        }
        requireModule(accessor);
        return Linked.annotation(annotationType, (AnnotationValue<A>) value);
    }

    /**
     * Builds an instance of a binding annotation the element carries, whose type is found by its name: through the
     * builder Micronaut generated for it, which knows its type, or as a class the metadata names.
     *
     * @param metadata The metadata of the element, which resolves the type of an annotation without a builder
     * @param value    The value Micronaut recorded for the annotation
     * @param accessor The accessor of {@code InvocationContext} that was called
     * @return The instance, or nothing where the type of the annotation is not on the classpath
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Optional<Annotation> annotation(AnnotationMetadata metadata, AnnotationValue<?> value, String accessor) {
        Optional<AnnotationBuilder<?>> builder = AnnotationBuilder.find(value.getAnnotationName());
        if (builder.isPresent()) {
            return Optional.of(builder.get().build(value));
        }
        // finding the class asks nothing of the reflection module, but building its instance does, so its absence
        // is reported before the type is looked for
        requireModule(accessor);
        return metadata.getAnnotationType(value.getAnnotationName())
            .map(type -> Linked.annotation((Class) type, (AnnotationValue) value));
    }

    /**
     * Makes sure the reflection module is on the classpath before it is used to answer an accessor.
     *
     * @param accessor The accessor of {@code InvocationContext} that was called
     * @throws UnsupportedOperationException naming the accessor and the dependency to add, where the module is absent
     */
    static void requireModule(String accessor) {
        NoClassDefFoundError missing = Module.MISSING;
        if (missing != null) {
            throw new UnsupportedOperationException("InvocationContext." + accessor + " returns an object of the Java"
                + " reflection API, which needs io.micronaut:micronaut-reflection on the classpath (or"
                + " io.micronaut.cdi:micronaut-cdi-reflection, which brings it, when the interceptors are used through"
                + " Micronaut CDI). Add that dependency, or read the interception through MicronautInvocationContext,"
                + " which reflects on nothing.", missing);
        }
    }

    /**
     * Whether the reflection module is on the classpath, found out the first time an accessor that needs it is
     * called - a class is initialized on its first use, so an application whose interceptors call none of those
     * accessors never looks for the module at all.
     */
    static final class Module {

        /**
         * What loading a class of the module failed with, or {@code null} where the module is there. It is the
         * cause of every failure that reports the module missing, which is the same for each of them.
         */
        @SuppressWarnings("StaticAssignmentOfThrowable")
        static final @Nullable NoClassDefFoundError MISSING = find();

        private Module() {
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

    /**
     * The calls into the reflection module, in a class of their own that links to it, so that it initializes only
     * once {@link #requireModule(String)} has found the module there.
     */
    private static final class Linked {

        static {
            // links this class to the reflection module: not every lookup below is answered by a class of that
            // module, and all of them are to fail alike without it
            Objects.requireNonNull(ReflectionExecutables.class);
        }

        private Linked() {
        }

        static Method method(MethodReference<?, ?> method) {
            return Objects.requireNonNull(ReflectionExecutables.targetMethod(method),
                "The intercepted method stands for no method of the platform");
        }

        static <A extends Annotation> A annotation(Class<A> annotationType, AnnotationValue<A> value) {
            return ReflectionAnnotations.synthesize(annotationType, value);
        }
    }
}
