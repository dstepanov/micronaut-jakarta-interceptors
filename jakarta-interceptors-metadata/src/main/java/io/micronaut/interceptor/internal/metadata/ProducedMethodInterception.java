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
 * The interception of one method of a bean a factory produces, recorded in {@link JakartaInterception#methods()} on
 * the member of the factory that produces it.
 *
 * <p>The interception of a method of an ordinary bean is written on the method itself. A method of a produced type
 * belongs to that type, and is the same method in every bean produced from it: a type a factory produces may be a
 * bean of its own as well, and two members may produce it binding different things, and an annotation written on the
 * method would be the interception of all of them. It may also be compiled apart from the factory, and then carries
 * nothing the processor would have written while the type itself was compiled. So what a method of the product
 * declares on itself is read while the factory is compiled, combined with what the member and the produced type
 * declare at class level, and recorded here, per member; the runtime reads it in place of whatever the method
 * itself carries.</p>
 *
 * <p>The members other than {@link #name()} and {@link #parameters()} are the members of the same name of
 * {@link JakartaInterception}, and mean the same.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Internal
public @interface ProducedMethodInterception {

    /**
     * The name of {@link #name()}.
     */
    String NAME = "name";

    /**
     * The name of {@link #parameters()}.
     */
    String PARAMETERS = "parameters";

    /**
     * The name of the method.
     *
     * @return The name
     */
    String name();

    /**
     * The types of the parameters of the method, each named as {@link Class#getTypeName()} names its erasure, which
     * tells an overload from another without loading a class.
     *
     * @return The names of the parameter types
     */
    String[] parameters() default {};

    /**
     * See {@link JakartaInterception#interceptors()}.
     *
     * @return The interceptor classes
     */
    Class<?>[] interceptors() default {};

    /**
     * See {@link JakartaInterception#excluded()}.
     *
     * @return Whether the method is excluded from interception
     */
    boolean excluded() default false;

    /**
     * See {@link JakartaInterception#timeout()}.
     *
     * @return Whether the method is a timeout method
     */
    boolean timeout() default false;

    /**
     * See {@link JakartaInterception#bindings()}.
     *
     * @return The bindings of the method
     */
    BoundBinding[] bindings() default {};
}
