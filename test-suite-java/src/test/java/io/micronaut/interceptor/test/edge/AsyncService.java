package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

@Singleton
@Alpha
public class AsyncService {

    public CompletableFuture<String> later() {
        return CompletableFuture.supplyAsync(() -> {
            Log.RECORDED.add("body ran");
            return "later";
        });
    }
}
