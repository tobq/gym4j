package tobi.gym;

public class CallbackMessage extends Message {
    private final Callback<byte[]> callback;

    public CallbackMessage(byte[] message, Callback<byte[]> callback) {
        super(message);
        this.callback = callback;
    }
    public CallbackMessage(String message, Callback<byte[]> callback) {
        super(message);
        this.callback = callback;
    }

    public Callback<byte[]> getCallback() {
        return callback;
    }
}

interface Callback<T> {
    void call(T response);
}