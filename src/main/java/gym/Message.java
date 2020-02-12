package gym;

public class Message {
    private final byte[] message;
    private final Callback<byte[]> callback;

    public Message( byte[] message, Callback<byte[]> callback) {
        this.message = message;
        this.callback = callback;
    }

    public byte[] getMessage() {
        return message;
    }

    public Callback<byte[]> getCallback() {
        return callback;
    }
}

interface Callback<T> {
    void call(T response);
}