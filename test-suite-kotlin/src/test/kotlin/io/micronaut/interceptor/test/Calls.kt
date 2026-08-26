package io.micronaut.interceptor.test

object Calls {

    val recorded: MutableList<String> = mutableListOf()

    fun clear() = recorded.clear()
}
