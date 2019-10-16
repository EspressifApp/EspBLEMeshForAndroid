package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;

public class GenericLevelSetMessage extends MeshMessage {
    private int mLevel;
    private boolean mUnacknowledged;

    public GenericLevelSetMessage(long dstAddress, Node node, App app, int level) {
        this(dstAddress, node, app, level, false);
    }

    public GenericLevelSetMessage(long dstAddress, Node node, App app, int level, boolean unacknowledged) {
        super(dstAddress, node, app);

        mLevel = level;
        mUnacknowledged = unacknowledged;
    }

    @Override
    public byte[] getOpCode() {
        if (mUnacknowledged) {
            return new byte[]{(byte) 0x82, 0x07};
        } else {
            return new byte[]{(byte) 0x82, 0x06};
        }
    }

    @Override
    public byte[] getParameters() {
        return new byte[] {
                (byte) (mLevel & 0xff), (byte) ((mLevel >> 8) & 0xff),
                (byte) System.nanoTime()
        };
    }

    @Override
    public int getProxyType() {
        return MeshConstants.PROXY_TYPE_NETWORK_PDU;
    }

    @Override
    public SecurityKey getSecurityKey() {
        return SecurityKey.AppKey;
    }
}
