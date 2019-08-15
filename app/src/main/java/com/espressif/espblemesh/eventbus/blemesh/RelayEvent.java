package com.espressif.espblemesh.eventbus.blemesh;

public class RelayEvent {
    private int mState;
    private int mCount;
    private int mStep;

    public RelayEvent(int state, int count, int step) {
        mState = state;
        mCount = count;
        mStep = step;
    }

    public int getState() {
        return mState;
    }

    public int getCount() {
        return mCount;
    }

    public int getStep() {
        return mStep;
    }
}
