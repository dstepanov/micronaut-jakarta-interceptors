package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
public class TaggedService {

    @Tags({})
    public String withNoTags() {
        return "none";
    }

    /**
     * Bound differently from {@link UntaggedInterceptor}: one tag, which happens to be empty, is not no tags.
     */
    @Tags({""})
    public String withOneEmptyTag() {
        return "one empty";
    }

    @Tags({"", ""})
    public String withTwoEmptyTags() {
        return "two empty";
    }
}
