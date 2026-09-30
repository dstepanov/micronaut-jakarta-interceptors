package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext
import org.junit.jupiter.api.Test

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * A binding a method declares is not inherited by a method that overrides it, where the overriding class is bound
 * itself as where it is not, as in Java.
 */
class OverriddenMethodBindingTest {

    @Test
    void theBindingOfAnOverriddenMethodDoesNotComeBackWhereTheClassIsBound() {
        ApplicationContext context = ApplicationContext.run()
        try {
            WatchCalls.RECORDED.clear()
            assertEquals("overridden", context.getBean(TaggedOverridingService).work())
            assertEquals(["tagged work"], WatchCalls.RECORDED)
        } finally {
            context.close()
        }
    }
}

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Tagged {
}

@Interceptor
@Tagged
class TaggingInterceptor {
    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        WatchCalls.RECORDED << "tagged ${context.method.name}".toString()
        return context.proceed()
    }
}

class WatchedBase {
    @Watched
    String work() {
        return "base"
    }
}

@Singleton
@Tagged
class TaggedOverridingService extends WatchedBase {
    @Override
    String work() {
        return "overridden"
    }
}
