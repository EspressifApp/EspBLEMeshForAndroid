package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;

public class LightCTLSetMessage extends Message {
    private int mLightness;
    private int mTemperature;
    private int mDeltaUV;

    public LightCTLSetMessage(long dstAddress, Node node, App app, int lightness, int temperature, int deltaUV) {
        super(dstAddress, node, app);

        mLightness = lightness;
        mTemperature = temperature;
        mDeltaUV = deltaUV;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x82, 0x5E};
    }

    @Override
    public byte[] getParameters() {
        byte tid = (byte) System.nanoTime();
        return new byte[]{
                (byte) (mLightness & 0xff), (byte) ((mLightness >> 8) & 0xff),
                (byte) (mTemperature & 0xff), (byte) ((mTemperature >> 8) & 0xff),
                (byte) (mDeltaUV & 0xff), (byte) ((mDeltaUV >> 8) & 0xff),
                tid
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
