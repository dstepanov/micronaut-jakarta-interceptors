# Micronaut Jakarta Interceptors

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.interceptor/micronaut-jakarta-interceptors.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.interceptor%22%20AND%20a:%22micronaut-jakarta-interceptors%22)

An implementation of the
[Jakarta Interceptors 2.2](https://jakarta.ee/specifications/interceptors/2.2/jakarta-interceptors-spec-2.2)
specification built on the compile-time aspect oriented programming of Micronaut.

An annotation processor finds and validates the interceptor methods while they are compiled, resolves the
interception of every element then and there, and lets Micronaut generate the proxies. At runtime an interceptor
method is a direct invocation of the executable method Micronaut generated for it — the interception never
reflects.

## Example

```java
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Logged {
}

@Interceptor
@Logged
public class LoggingInterceptor {

    @AroundInvoke
    public Object log(InvocationContext context) throws Exception {
        System.out.println("entering " + context.getMethod().getName());
        return context.proceed();
    }
}

@Singleton
@Logged
public class Greeter {

    public String greet(String name) {
        return "Hello " + name;
    }
}
```

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-jakarta-interceptors/latest/guide/) for more
information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-jakarta-interceptors/snapshot/guide/)
for the current development docs.

## Snapshots and Releases

Snapshots are automatically published to [Sonatype Snapshots](https://central.sonatype.com/repository/maven-snapshots/)
using [GitHub Actions](https://github.com/micronaut-projects/micronaut-jakarta-interceptors/actions).

Releases are published to Maven Central.
