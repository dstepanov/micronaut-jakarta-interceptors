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
package io.micronaut.interceptor.test.library;

import java.util.List;

/**
 * A class of the library that declares its binding on one of its methods alone, and is no bean of its own.
 */
public class MethodBoundLibraryService {

    @LibraryBinding
    public String bound(List<String> calls) {
        calls.add("bound");
        return "bound";
    }

    public String unbound(List<String> calls) {
        calls.add("unbound");
        return "unbound";
    }
}
