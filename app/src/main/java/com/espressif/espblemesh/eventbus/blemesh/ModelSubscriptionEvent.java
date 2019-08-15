package com.espressif.espblemesh.eventbus.blemesh;

public class ModelSubscriptionEvent {
    private int mStatus;
    private long mGroupAddr;
    private String mNodeMac;
    private long mElementAddr;
    private String mModelId;

    public ModelSubscriptionEvent(int status, long groupAddr, String nodeMac, long elementAddr, String modelId) {
        mStatus = status;
        mGroupAddr = groupAddr;
        mNodeMac = nodeMac;
        mElementAddr = elementAddr;
        mModelId = modelId;
    }

    public int getStatus() {
        return mStatus;
    }

    public long getGroupAddr() {
        return mGroupAddr;
    }

    public String getNodeMac() {
        return mNodeMac;
    }

    public long getElementAddr() {
        return mElementAddr;
    }

    public String getModelId() {
        return mModelId;
    }
}
