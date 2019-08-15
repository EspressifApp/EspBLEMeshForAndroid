package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;

public class CompositionDataGetMessage extends Message {
    private int mPage;

    public CompositionDataGetMessage(Node node, int page) {
        super(node.getUnicastAddress(), node);

        mPage = page;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x80, 0x08};
    }

    @Override
    public byte[] getParameters() {
        return new byte[]{(byte) mPage};
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
