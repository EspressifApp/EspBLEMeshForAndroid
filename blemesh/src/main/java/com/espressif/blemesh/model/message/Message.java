package com.espressif.blemesh.model.message;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;

public abstract class Message {
    private static final int CTL_DEFAULT = 0;
    private static final int TTL_DEFAULT = 10;

    private static final int POST_COUNT_DEFAULT = 1;

    private int mCTL = CTL_DEFAULT;
    private int mTTL = TTL_DEFAULT;

    private int mPostCount = POST_COUNT_DEFAULT;

    private final long mDstAddress;
    private final Node mNode;
    private App mApp;

    public Message(long dstAddress) {
        this(dstAddress,null, null);
    }

    public Message(long dstAddress, Node node) {
        this(dstAddress, node, null);
    }

    public Message(long dstAddress, Node node, App app) {
        mDstAddress = dstAddress;
        mNode = node;
        mApp = app;
    }

    /**
     * Custom OPCODE:
     * [0xC0, 0xE5, 0x02] : Set fast provision info
     * [0xC1, 0xE5, 0x02] : Receive fast provision status
     * [0xC6, 0xE5, 0x02] : Get fast provision nodes' address
     * [0xC7, 0xE5, 0x02] : Receive fast provision nodes' address status
     * [0xC8, 0xE5, 0x02] : Bind nodes' address in group
     * [0xC9, 0xE5, 0x02] : Unbind nodes' address from group
     * [0xCA, manufacturerId[0], manufacturerId[1]] : OTA NBVN
     * [0xCB, manufacturerId[0], manufacturerId[1]] : OTA Need Update Notification
     * [0xCC, manufacturerId[0], manufacturerId[1]] : OTA Start
     * [0xCD, manufacturerId[0], manufacturerId[1]] : OTA Start Response
     */
    public abstract byte[] getOpCode();

    public abstract byte[] getParameters();

    /**
     * Get Proxy type
     *
     * @return {@link MeshConstants#PROXY_TYPE_NETWORK_PDU}, {@link MeshConstants#PROXY_TYPE_MESH_BEACON},
     * {@link MeshConstants#PROXY_TYPE_PROXY_CONGURATION} or {@link MeshConstants#PROXY_TYPE_PROVISIONING_PDU}
     */
    public abstract int getProxyType();

    public abstract SecurityKey getSecurityKey();

    public void setCTL(int CTL) {
        mCTL = CTL;
    }

    public int getCTL() {
        return mCTL;
    }

    public void setTTL(int TTL) {
        mTTL = TTL;
    }

    public int getTTL() {
        return mTTL;
    }

    public long getDstAddress() {
        return mDstAddress;
    }

    public Node getNode() {
        return mNode;
    }

    protected void setApp(App app) {
        mApp = app;
    }

    public App getApp() {
        return mApp;
    }

    public void setPostCount(int postCount) {
        mPostCount = postCount;
    }

    public int getPostCount() {
        return mPostCount;
    }

    public enum SecurityKey {
        DeviceKey, AppKey, NetEncryptionKey
    }
}
