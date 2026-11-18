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

import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The type of a binding annotation is the one of the application's class loader, which may define a copy of its own
 * of a type the class loader of the thread also sees, as a deployment beneath a Micronaut shared with others does.
 */
class BindingAnnotationAccessTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Deployed {
    }

    @Test
    void theTypeOfABindingIsFoundInTheClassLoaderOfTheApplication() throws Exception {
        String name = Deployed.class.getName();
        ClassLoader deployment = new ChildFirst(name, BindingAnnotationAccessTest.class.getClassLoader());
        Class<?> deployed = deployment.loadClass(name);
        assertNotSame(Deployed.class, deployed);

        Class<? extends Annotation> type = BindingAnnotationAccess.bindingType(
            new MutableAnnotationMetadata(), name, deployment, "getInterceptorBindings()").orElseThrow();

        assertSame(deployed, type);
    }

    /**
     * Defines one class itself, from the class file its parent has, and delegates every other one.
     */
    private static final class ChildFirst extends ClassLoader {

        private final String own;

        ChildFirst(String own, ClassLoader parent) {
            super(parent);
            this.own = own;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(own)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }
                try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    byte[] bytes = in.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
    }
}
