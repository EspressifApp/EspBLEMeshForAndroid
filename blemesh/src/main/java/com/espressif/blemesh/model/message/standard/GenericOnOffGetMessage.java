package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;

import java.util.Random;

public class GenericOnOffGetMessage extends MeshMessage {
    private boolean mOn;

    public GenericOnOffGetMessage(long dstAddr, Node node, App app) {
        super(dstAddr, node, app);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x82, 0x01};
    }

    @Override
    public byte[] getParameters() {
        byte state = (byte) (mOn ? 0x01 : 0x00);
        byte tid = (byte) new Random().nextInt();
        byte transitionTime = 0x01;
        byte delay = 0x00;
        return new byte[]{state, tid};//new byte[]{state, tid, transitionTime, delay};
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
