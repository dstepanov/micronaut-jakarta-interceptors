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
package io.micronaut.interceptor.annotation;

import io.micronaut.core.annotation.Internal;

/**
 * The type the interceptor classes of an application are found through.
 *
 * <p>The processor declares {@code @Indexed} of this type on every interceptor class, and the runtime asks the
 * context for the definitions of this type to find them, instead of reading every bean definition there is and
 * keeping the ones that are interceptor classes.</p>
 *
 * <p>An interceptor class of the specification implements nothing, and none of them implements this: a bean is
 * enumerable by a type it is indexed by whether or not it is one, which is all the index is for. Nothing is ever
 * resolved as an instance of it.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public interface JakartaInterceptorIndex {
}
