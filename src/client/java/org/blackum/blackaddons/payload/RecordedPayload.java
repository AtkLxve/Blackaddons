package org.blackum.blackaddons.payload;

public class RecordedPayload {
    public String channel;
    public String data;
    public long timestamp;

    public RecordedPayload(String channel, String data, long timestamp) {
        this.channel = channel;
        this.data = data;
        this.timestamp = timestamp;
    }
}
