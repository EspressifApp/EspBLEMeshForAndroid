package com.espressif.blemesh.model.message.standard.proxyconfiguration;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;

public class SetFilterTypeMessage extends ProxyConfigurationMessage {
    private int mFilterType;

    public SetFilterTypeMessage(Node node, int filterType) {
        super(node);

        mFilterType = filterType;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{0x00};
    }

    @Override
    public byte[] getParameters() {
        return new byte[]{(byte) mFilterType};
    }

    @Override
    public int getProxyType() {
        return MeshConstants.PROXY_TYPE_PROXY_CONGURATION;
    }

    @Override
    public SecurityKey getSecurityKey() {
        return SecurityKey.NetEncryptionKey;
    }
}
