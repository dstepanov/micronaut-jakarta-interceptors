package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
public class PunctuatedService {

    /**
     * Bound differently from {@link PunctuatedInterceptor}, but only in where the punctuation of one of the values
     * falls: written out without escaping, both read as {@code first=a,second=b,second=c}.
     */
    @Tagged(first = "a", second = "b,second=c")
    public String looksLikeTheInterceptor() {
        return "different";
    }

    @Tagged(first = "a,second=b", second = "c")
    public String isTheInterceptor() {
        return "same";
    }
}
