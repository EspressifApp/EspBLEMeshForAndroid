package com.espressif.blemesh.model.message.standard.proxyconfiguration;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;

import java.util.Collection;

public class AddAddressesToFilterMessage extends ProxyConfigurationMessage {
    private Collection<byte[]> mAddresses;

    public AddAddressesToFilterMessage(Node node, Collection<byte[]> addresses) {
        super(node);

        mAddresses= addresses;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{0x01};
    }

    @Override
    public byte[] getParameters() {
        byte[] parameters = new byte[mAddresses.size() * 2];
        int i = 0;
        for (byte[] addr : mAddresses) {
            int offset = i * 2;
            parameters[offset + 1] = addr[0];
            parameters[offset] = addr[1];

            i++;
        }

        return parameters;
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
