package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;

public class RelaySetMessage extends MeshMessage {
    private boolean mEnable;
    private int mCount;
    private int mStep;

    public RelaySetMessage(Node node, boolean enable, int count, int step) {
        super(node.getUnicastAddress(), node);

        mEnable = true;
        mCount = count;
        mStep = step;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x80, 0x27};
    }

    @Override
    public byte[] getParameters() {
        byte state = (byte) (mEnable ? 0x01 : 0x00);
        byte retransmit = (byte) ((mStep << 3) | mCount);
        return new byte[]{state, retransmit};
    }

    @Override
    public int getProxyType() {
        return MeshConstants.PROXY_TYPE_NETWORK_PDU;
    }

    @Override
    public SecurityKey getSecurityKey() {
        return SecurityKey.DeviceKey;
    }
}
