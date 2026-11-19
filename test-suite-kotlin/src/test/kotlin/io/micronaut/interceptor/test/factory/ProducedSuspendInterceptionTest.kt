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
package io.micronaut.interceptor.test.factory

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.interceptor.test.Calls
import io.micronaut.interceptor.test.Logged
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

open class SuspendingProduct {

    @Logged
    open suspend fun greet(name: String): String {
        Calls.recorded += "greet"
        return "Hello $name"
    }

    open suspend fun plain(name: String): String {
        Calls.recorded += "plain"
        return "Hi $name"
    }
}

@Factory
class SuspendingProductFactory {

    @Singleton
    fun product(): SuspendingProduct = SuspendingProduct()
}

class ProducedSuspendInterceptionTest {

    /**
     * Micronaut compiles a suspending function of a produced bean with the continuation the Kotlin compiler adds
     * after the parameters it declares, and the binding the function declares is still found for it.
     */
    @Test
    fun aBindingOfASuspendingFunctionOfAProducedBeanIsKept() {
        ApplicationContext.run().use { context ->
            Calls.clear()
            val product = context.getBean(SuspendingProduct::class.java)

            assertEquals("Hello Denis", runBlocking { product.greet("Denis") })
            assertEquals(listOf("log in greet", "greet", "log out"), Calls.recorded)

            Calls.clear()
            assertEquals("Hi Denis", runBlocking { product.plain("Denis") })
            assertEquals(listOf("plain"), Calls.recorded)
        }
    }
}
