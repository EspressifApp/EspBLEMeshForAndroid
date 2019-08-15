package com.espressif.espblemesh.eventbus;

import com.espressif.blemesh.client.IMeshMessager;

public class GattNodeServiceEvent {
    private int mCode;
    private IMeshMessager mMessager;

    public GattNodeServiceEvent(int code, IMeshMessager messager) {
        mCode = code;
        mMessager = messager;
    }

    public int getCode() {
        return mCode;
    }

    public IMeshMessager getMessager() {
        return mMessager;
    }
}
