package com.espressif.blemesh.model;

import com.espressif.blemesh.utils.MeshUtils;

public class App {
    private byte[] mAppKey;
    private int mAid;

    private long mKeyIndex;

    private long mUnicastAddr;

    public App(byte[] appKey, long keyIndex) {
        setAppKey(appKey);
        mKeyIndex = keyIndex;
    }

    public byte[] getAppKey() {
        return mAppKey;
    }

    public void setAppKey(byte[] appKey) {
        mAppKey = appKey;
        mAid = MeshUtils.getAID(mAppKey)[0] & 63;
    }

    public int getAid() {
        return mAid;
    }

    public long getKeyIndex() {
        return mKeyIndex;
    }

    public void setKeyIndex(long keyIndex) {
        mKeyIndex = keyIndex;
    }

    public void setUnicastAddr(long unicastAddr) {
        mUnicastAddr = unicastAddr;
    }

    public long getUnicastAddr() {
        return mUnicastAddr;
    }
}
