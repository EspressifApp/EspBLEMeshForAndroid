package com.espressif.espblemesh.eventbus.blemesh;

public class CompositionDataEvent {
    private int mStatus;
    private int mPage;

    public CompositionDataEvent(int status, int page) {
        mStatus = status;
        mPage = page;
    }

    public int getStatus() {
        return mStatus;
    }

    public int getPage() {
        return mPage;
    }
}
