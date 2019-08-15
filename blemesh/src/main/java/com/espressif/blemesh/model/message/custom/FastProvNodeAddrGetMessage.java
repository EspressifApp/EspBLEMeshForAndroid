package com.espressif.blemesh.model.message.custom;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;

public class FastProvNodeAddrGetMessage extends Message {
    public FastProvNodeAddrGetMessage(Node node, App app) {
        super(node.getUnicastAddress(), node, app);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0xC6, (byte) 0xE5, 0x02};
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
