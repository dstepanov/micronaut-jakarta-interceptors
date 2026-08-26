plugins {
    id("io.micronaut.build.internal.jakarta-interceptors-test-suite")
    id("org.jetbrains.kotlin.jvm")
    id("io.micronaut.build.internal.kotlin-base")
    id("io.micronaut.build.internal.kotlin-ksp")
}

dependencies {
    kspTest(projects.micronautJakartaInterceptorsProcessor)
    kspTest(mn.micronaut.inject.kotlin)

    testImplementation(platform(libs.micronaut.core))
    testImplementation(projects.micronautJakartaInterceptors)
    testImplementation(mn.micronaut.core)
    testImplementation(mn.micronaut.inject)
    testImplementation(mn.micronaut.aop)
    testRuntimeOnly(mn.micronaut.context)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
