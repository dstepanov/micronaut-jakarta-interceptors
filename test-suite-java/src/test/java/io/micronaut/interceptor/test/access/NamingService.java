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

import jakarta.inject.Singleton;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptors;

/**
 * A bean that names an interceptor class at class level and has a protected method excluding it, which is what
 * shows that a protected method still takes part in the exclusion rules.
 */
@Singleton
@Interceptors(WatchingInterceptor.class)
public class NamingService {

    protected String prot() {
        Calls.RECORDED.add("prot");
        return "prot";
    }

    @ExcludeClassInterceptors
    protected String excluding() {
        Calls.RECORDED.add("excluding");
        return "excluding";
    }
}
