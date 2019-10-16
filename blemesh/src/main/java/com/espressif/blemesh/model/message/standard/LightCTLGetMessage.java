package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;

public class LightCTLGetMessage extends MeshMessage {
    public LightCTLGetMessage(long dstAddress, Node node, App app) {
        super(dstAddress, node, app);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x82, 0x5D};
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
        return SecurityKey.AppKey;
    }
}
