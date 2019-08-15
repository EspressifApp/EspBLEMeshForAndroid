package com.espressif.blemesh.model;

import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import libs.espressif.utils.DataUtil;

public class Node {
    private String mMac;
    private String mUUID;
    private String mName;
    private byte[] mDeviceKey;
    private long mUnicastAddress;
    private int mElementCount;
    private long mNetKeyIndex;

    private Long mCid;
    private Long mPid;
    private Long mVid;
    private Long mCrpl;
    private Long mFeatures;

    private final LongSparseArray<Element> mElements = new LongSparseArray<>();

    private final Set<Long> mAppKeySet = new HashSet<>();

    public String getMac() {
        return mMac;
    }

    public void setMac(String mac) {
        mMac = mac;
    }

    public String getUUID() {
        return mUUID;
    }

    public void setUUID(String UUID) {
        mUUID = UUID;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public byte[] getDeviceKey() {
        return mDeviceKey;
    }

    public void setDeviceKey(byte[] deviceKey) {
        mDeviceKey = deviceKey;
    }

    public long getUnicastAddress() {
        return mUnicastAddress;
    }

    public void setUnicastAddress(long unicastAddress) {
        mUnicastAddress = unicastAddress;
    }

    public int getElementCount() {
        return mElementCount;
    }

    public void setElementCount(int elementCount) {
        mElementCount = elementCount;
    }

    public long getNetKeyIndex() {
        return mNetKeyIndex;
    }

    public void setNetKeyIndex(long netKeyIndex) {
        mNetKeyIndex = netKeyIndex;
    }

    public void setCid(Long cid) {
        mCid = cid;
    }

    public Long getCid() {
        return mCid;
    }

    public void setPid(Long pid) {
        mPid = pid;
    }

    public Long getPid() {
        return mPid;
    }

    public void setVid(Long vid) {
        mVid = vid;
    }

    public Long getVid() {
        return mVid;
    }

    public void setCrpl(Long crpl) {
        mCrpl = crpl;
    }

    public Long getCrpl() {
        return mCrpl;
    }

    public void setFeatures(Long features) {
        mFeatures = features;
    }

    public Long getFeatures() {
        return mFeatures;
    }

    public boolean hasCompositionData() {
        return mElements.size() == mElementCount
                && mCid != null
                && mPid != null
                && mVid != null
                && mCrpl != null
                && mFeatures != null;
    }

    public List<Element> getElementList() {
        synchronized (mElements) {
            List<Element> list = new ArrayList<>(mElements.size());
            for (int i = 0; i < mElements.size(); i++) {
                list.add(mElements.valueAt(i));
            }
            return list;
        }
    }

    public Element getElementForAddress(long elementAddress) {
        synchronized (mElements) {
            return mElements.get(elementAddress);
        }
    }

    public void addElement(Element element) {
        synchronized (mElements) {
            mElements.put(element.getUnicastAddress(), element);
        }
    }

    public void addAppKeyIndex(long appKeyIndex) {
        synchronized (mAppKeySet) {
            mAppKeySet.add(appKeyIndex);
        }
    }

    public void removeAppKeyIndex(long appKeyIndex) {
        synchronized (mAppKeySet) {
            mAppKeySet.remove(appKeyIndex);
        }
    }

    public boolean containsAppKeyIndex(long appKeyIndex) {
        synchronized (mAppKeySet) {
            return mAppKeySet.contains(appKeyIndex);
        }
    }

    @Override
    public String toString() {
        return "Node: " +
                "Mac = " + mMac + ", " +
                "Name = " + mName + ", " +
                "DeviceKey = " + DataUtil.bigEndianBytesToHexString(mDeviceKey) + ", " +
                "UnicastAddress = " + mUnicastAddress + ", " +
                "ElementNumber = " + mElementCount + ", " +
                "cid = " + mCid + ", " +
                "pid = " + mPid + ", " +
                "vid = " + mVid + ", " +
                "crpl = " + mCrpl + ", " +
                "features = " + mFeatures;
    }
}
