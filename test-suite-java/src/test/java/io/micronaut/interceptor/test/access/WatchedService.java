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

/**
 * A bean bound at class level whose business methods cover every access level a business method may have.
 */
@Singleton
@Watched
public class WatchedService {

    public String pub() {
        Calls.RECORDED.add("pub");
        return "pub";
    }

    protected String prot() {
        Calls.RECORDED.add("prot");
        return "prot";
    }

    String pkg() {
        Calls.RECORDED.add("pkg");
        return "pkg";
    }

    public String callsProtected() {
        Calls.RECORDED.add("callsProtected");
        return prot();
    }
}
