package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
public class ShelvedService {

    @Shelved
    public String leftToItsDefaults() {
        return "defaults";
    }

    @Shelved("")
    public String emptyValueSpeltOut() {
        return "empty value";
    }

    @Shelved(value = "", labels = {})
    public String everyDefaultSpeltOut() {
        return "every default";
    }

    @Shelved("top")
    public String onANamedShelf() {
        return "named";
    }
}
