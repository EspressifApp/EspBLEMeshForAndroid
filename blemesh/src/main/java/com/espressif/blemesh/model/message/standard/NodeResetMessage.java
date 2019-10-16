package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;

public class NodeResetMessage extends MeshMessage {
    public NodeResetMessage(Node node, long dstAddress) {
        super(dstAddress, node);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x80, 0x49};
    }

    @Override
    public byte[] getParameters() {
        return new byte[0];
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
