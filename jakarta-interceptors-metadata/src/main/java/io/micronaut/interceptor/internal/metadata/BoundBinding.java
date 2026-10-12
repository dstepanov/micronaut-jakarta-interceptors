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
package io.micronaut.interceptor.internal.metadata;

import io.micronaut.core.annotation.Internal;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The element type of the members that record interceptor bindings, {@link JakartaInterception#bindings()},
 * {@link JakartaInterception#associatedBindings()} and {@link JakartaInterceptorMethods#bindings()}.
 *
 * <p>Nothing is recorded as this annotation. Each element of those members is the value of a binding annotation
 * itself, named by the binding annotation, as the processor reduced it to what it binds by: the members it defaults
 * to filled in, the members excluded from the binding left out and listed under {@code $nonBinding}, an enum
 * constant recorded by its name and a class as a class value. An annotation member has to declare some annotation
 * type as its element type, and the binding annotations are not known until an application is compiled, so this
 * one stands in for them. The values are read as annotation values, and nothing builds an instance of this.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Internal
public @interface BoundBinding {
}
