package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

import java.util.concurrent.CopyOnWriteArrayList

class GovernCalls {
    static final List<String> RECORDED = new CopyOnWriteArrayList<>()
}

@Interceptor
@Governed(primary = @Rule(name = "main", note = "of the interceptor"),
    clauses = [@Clause(name = "first"), @Clause(name = "second", note = "x")])
class GoverningInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        GovernCalls.RECORDED.add("governed " + context.method.name)
        return context.proceed()
    }
}

@Singleton
class GovernedService {

    /** Differs from the interceptor only in the members that do not bind, in the single member and in the array. */
    @Governed(primary = @Rule(name = "main", note = "of the service"),
        clauses = [@Clause(name = "first", note = "y"), @Clause(name = "second")])
    String work() { "work" }

    /** Differs from the interceptor in a member of {@link Rule} that binds. */
    @Governed(primary = @Rule(name = "other"), clauses = [@Clause(name = "first"), @Clause(name = "second")])
    String other() { "other" }

    /** Agrees with the interceptor in the single member, and differs in a member of an element of the array. */
    @Governed(primary = @Rule(name = "main"), clauses = [@Clause(name = "first"), @Clause(name = "third")])
    String otherClause() { "other clause" }
}
