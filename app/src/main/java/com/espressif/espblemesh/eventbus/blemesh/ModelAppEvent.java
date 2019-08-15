package com.espressif.espblemesh.eventbus.blemesh;

public class ModelAppEvent {
    private int mStatus;
    private long mAppKeyIndex;
    private String mNodeMac;
    private long mElementAddr;
    private String mModeId;

    public ModelAppEvent(int status, long appKeyIndex, String nodeMac, long elementAddr, String modeId) {
        mStatus = status;
        mAppKeyIndex = appKeyIndex;
        mNodeMac = nodeMac;
        mElementAddr = elementAddr;
        mModeId = modeId;
    }

    public int getStatus() {
        return mStatus;
    }

    public long getAppKeyIndex() {
        return mAppKeyIndex;
    }

    public String getNodeMac() {
        return mNodeMac;
    }

    public long getElementAddr() {
        return mElementAddr;
    }

    public String getModeId() {
        return mModeId;
    }
}
