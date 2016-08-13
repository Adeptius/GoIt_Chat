package chat.util;

import java.io.Serializable;

public class Message implements Serializable {

    private MessageType type;
    private String fileName;
    private String data;
    private byte[] bytesArray;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String data, byte[] bytesArray, String fileName) {
        this.bytesArray = bytesArray;
        this.type = type;
        this.fileName = fileName;
        this.data = data;
    }

    public Message(MessageType type, String data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public byte[] getBytesArray() {
        return bytesArray;
    }

    public String getFileName() {
        return fileName;
    }


}
