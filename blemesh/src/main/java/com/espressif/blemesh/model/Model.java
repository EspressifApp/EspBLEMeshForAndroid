package com.espressif.blemesh.model;

public class Model {
    private String mId;
    private long mElementAddress;
    private String mNodeMac;
    private long mAppKeyIndex = -1L;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public long getElementAddress() {
        return mElementAddress;
    }

    public void setElementAddress(long elementAddress) {
        mElementAddress = elementAddress;
    }

    public String getNodeMac() {
        return mNodeMac;
    }

    public void setNodeMac(String nodeMac) {
        mNodeMac = nodeMac;
    }

    public long getAppKeyIndex() {
        return mAppKeyIndex;
    }

    public void setAppKeyIndex(long appKeyIndex) {
        mAppKeyIndex = appKeyIndex;
    }

    public boolean hasAppKey() {
        return mAppKeyIndex >= 0;
    }
}
