package io.micronaut.interceptor.test

import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import jakarta.annotation.PostConstruct

@Prototype
@Compiled(region = "users")
open class CompiledService(@param:Parameter private val name: String) {

    @PostConstruct
    open fun started() {
    }

    open fun greet(greeting: String): String = "$greeting $name"
}
