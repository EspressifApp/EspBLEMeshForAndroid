package com.espressif.blemesh.model;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.utils.MeshAlgorithmUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import libs.espressif.utils.DataUtil;

public class Network {
    private byte[] mNetKey;
    private byte mNid;
    private byte[] mEncryptionKey;
    private byte[] mPrivacyKey;
    private byte[] mIdentityKey;

    private String mName;
    private long mKeyIndex;
    private long mIVIndex;
    private long mSeq;

    private final Set<String> mNodeMacSet;
    private final Set<Long> mGroupAddrSet;

    public Network(byte[] netKey, long keyIndex, String name, long ivIndex, long seq) {
        mNetKey = netKey;
        setNetKeyOther();

        mKeyIndex = keyIndex;
        mName = name;
        mIVIndex = ivIndex;
        mSeq = seq;

        mNodeMacSet = new HashSet<>();
        mGroupAddrSet = new HashSet<>();
    }

    private void setNetKeyOther() {
        byte[] netTemp = MeshAlgorithmUtils.k2(mNetKey, new byte[]{0x00});
        mNid = netTemp[0];
        mEncryptionKey = new byte[16];
        System.arraycopy(netTemp, 1, mEncryptionKey, 0, 16);
        mPrivacyKey = new byte[16];
        System.arraycopy(netTemp, 17, mPrivacyKey, 0, 16);

        byte[] salt = MeshAlgorithmUtils.s1("nkik".getBytes());
        byte[] P = DataUtil.mergeBytes("id128".getBytes(), new byte[]{0x01});
        mIdentityKey = MeshAlgorithmUtils.k1(mNetKey, salt, P);
    }

    public byte[] getNetKey() {
        return mNetKey;
    }

    public void setNetKey(byte[] netKey) {
        mNetKey = netKey;
        setNetKeyOther();
    }

    public byte getNid() {
        return mNid;
    }

    public byte[] getEncryptionKey() {
        return mEncryptionKey;
    }

    public byte[] getPrivacyKey() {
        return mPrivacyKey;
    }

    public byte[] getIdentityKey() {
        return mIdentityKey;
    }

    public long getKeyIndex() {
        return mKeyIndex;
    }

    public void setKeyIndex(long keyIndex) {
        mKeyIndex = keyIndex;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public void addNode(String nodeMac) {
        mNodeMacSet.add(nodeMac);
    }

    public void removeNode(String nodeMac) {
        mNodeMacSet.remove(nodeMac);
    }

    public boolean containsNode(String nodeMac) {
        return mNodeMacSet.contains(nodeMac);
    }

    public List<String> getNodeMacList() {
        return new ArrayList<>(mNodeMacSet);
    }

    public void addGroup(long groupAddr) {
        mGroupAddrSet.add(groupAddr);
    }

    public void removeGroup(long groupAddr) {
        mGroupAddrSet.remove(groupAddr);
    }

    public List<Long> getGroupAddressList() {
        return new ArrayList<>(mGroupAddrSet);
    }

    public long getIVIndex() {
        return mIVIndex;
    }

    public void setIVIndex(long IVIndex) {
        mIVIndex = IVIndex;
    }

    public byte[] getIVIndexBytes() {
        return new byte[] {
                (byte) ((mIVIndex >> 24) & 0xff),
                (byte) ((mIVIndex >> 16) & 0xff),
                (byte) ((mIVIndex >> 8) & 0xff),
                (byte) (mIVIndex & 0xff),
        };
    }

    public long getSeq() {
        return mSeq;
    }

    public long seqIncrementAndGet() {
        mSeq++;
        if (mSeq > 0xffffff) {
            mSeq = 0;
            mIVIndex++;
        }
        MeshObjectBox.getInstance().updateNetworkSeq(mKeyIndex, mSeq, mIVIndex);

        return mSeq;
    }
}
