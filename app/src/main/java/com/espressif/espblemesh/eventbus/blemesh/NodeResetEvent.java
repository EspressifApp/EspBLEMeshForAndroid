package com.espressif.espblemesh.eventbus.blemesh;

public class NodeResetEvent {
    public final long srcAddress;
    public final String nodeMac;

    public NodeResetEvent(long srcAddress, String nodeMac) {
        this.srcAddress = srcAddress;
        this.nodeMac = nodeMac;
    }
}
