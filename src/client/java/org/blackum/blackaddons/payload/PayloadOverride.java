package org.blackum.blackaddons.payload;

public class PayloadOverride {
    public String channel;
    public String originalData;
    public String replacementData;
    public boolean enabled;

    public PayloadOverride() {
    }

    public PayloadOverride(String channel, String originalData, String replacementData, boolean enabled) {
        this.channel = channel;
        this.originalData = originalData;
        this.replacementData = replacementData;
        this.enabled = enabled;
    }
}
