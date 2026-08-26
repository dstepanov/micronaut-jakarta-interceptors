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
package io.micronaut.interceptor.tck;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deployments of {@code org.jboss.cdi.tck.interceptors.tests.bindings.broken}, which the kit requires a
 * container to reject with a definition error.
 *
 * <p>A deployment has no counterpart here: nothing is deployed, and what a container would find as it started is
 * found by the annotation processor as the classes are compiled. Each deployment is therefore handed to the
 * compiler, with the annotation processor of this module on it, and rejected there instead. The classes of a
 * deployment are the ones its test class lists.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
class BrokenDeploymentTckTest {

    private static final String PACKAGE = "org.jboss.cdi.tck.interceptors.tests.bindings.broken";

    /**
     * The deployment of {@code InvalidTransitiveInterceptorBindingAnnotationsTest}: {@code Foo} declares
     * {@code @FooBinding} and {@code @BarBinding}, and each of those declares {@code @BazBinding} with the other
     * answer. Section 3.4.2 d).
     */
    @Test
    void aTransitiveBindingWithConflictingMembersIsRejected() {
        String error = compile("Foo", "FooBinding", "BarBinding", "BazBinding",
            "FooInterceptor", "BarInterceptor", "YesBazInterceptor", "NoBazInterceptor");
        assertTrue(error.contains("BazBinding"), error);
    }

    /**
     * The deployment of {@code InvalidStereotypeInterceptorBindingAnnotationsTest}, which reaches the same
     * conflict through two annotations that Contexts and Dependency Injection calls stereotypes. Nothing here
     * reads a stereotype as one; what makes the deployment invalid is that both carry {@code @BazBinding}, which
     * is what is found.
     */
    @Test
    void aBindingReachedThroughTwoStereotypesWithConflictingMembersIsRejected() {
        String error = compile("Bar", "FooBinding", "BarBinding", "BazBinding",
            "FooInterceptor", "BarInterceptor", "YesBazInterceptor", "NoBazInterceptor",
            "FooStereotype", "BarStereotype");
        assertTrue(error.contains("BazBinding"), error);
    }

    private static String compile(String... classes) {
        JavaFileObject[] sources = new JavaFileObject[classes.length];
        for (int i = 0; i < classes.length; i++) {
            sources[i] = sourceOf(classes[i]);
        }
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class, () -> parser.generate(sources));
            return failure.getMessage();
        }
    }

    /**
     * Reads a class of the deployment back from the sources the kit was unpacked from.
     */
    private static JavaFileObject sourceOf(String simpleName) {
        String path = "/" + PACKAGE.replace('.', '/') + "/" + simpleName + ".java";
        try (InputStream sources = BrokenDeploymentTckTest.class.getResourceAsStream(path)) {
            if (sources == null) {
                throw new IllegalStateException("The deployment class [" + path + "] was not unpacked");
            }
            String text = new String(sources.readAllBytes(), StandardCharsets.UTF_8);
            return new SimpleJavaFileObject(URI.create("string://" + path), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return text;
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
