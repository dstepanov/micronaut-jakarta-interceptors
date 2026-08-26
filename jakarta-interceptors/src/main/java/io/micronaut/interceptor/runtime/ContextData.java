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
package io.micronaut.interceptor.runtime;

import io.micronaut.core.annotation.Internal;

import java.util.HashMap;

/**
 * The map returned by {@code InvocationContext.getContextData()}.
 *
 * <p>It is a plain map; the type exists so that the map the interceptors of one invocation share can be told apart
 * from any other attribute of the Micronaut interceptor chain.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ContextData extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    ContextData() {
        super(4);
    }
}
