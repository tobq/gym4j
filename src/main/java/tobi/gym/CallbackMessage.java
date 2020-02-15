package tobi.gym;

import java.util.concurrent.CompletableFuture;

public class CallbackMessage extends Message {
    private final CompletableFuture<byte[]> callback;

    public CallbackMessage(byte[] message, CompletableFuture<byte[]> callback) {
        super(message);
        this.callback = callback;
    }
    public CallbackMessage(String message, CompletableFuture<byte[]> callback) {
        super(message);
        this.callback = callback;
    }

    public CompletableFuture<byte[]> getCallback() {
        return callback;
    }
}

interface Callback<T> {
    void call(T response);
}