package com.espressif.espblemesh.eventbus.blemesh;

public class FastProvAddrEvent {
    private String mNodeMac;
    private long[] mAddrArray;

    public FastProvAddrEvent(String nodeMac, long[] addrArray) {
        mNodeMac = nodeMac;
        mAddrArray = addrArray;
    }

    public String getNodeMac() {
        return mNodeMac;
    }

    public long[] getAddrArray() {
        return mAddrArray;
    }
}
