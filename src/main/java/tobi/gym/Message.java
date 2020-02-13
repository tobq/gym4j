package tobi.gym;

import java.nio.charset.StandardCharsets;

public class Message {
    protected final byte[] message;

    public Message(byte[] message) {
        this.message = message;
    }

    public Message(String message) {
        this(message.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getMessage() {
        return message;
    }
}
