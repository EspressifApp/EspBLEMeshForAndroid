package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;
import com.espressif.blemesh.utils.MeshUtils;

public class LightHSLSetMessage extends MeshMessage {
    private float[] mHSL;

    private boolean mUnacknowledged;

    /**
     * @param hsl h: [0 .. 360), s: [0...1], l: [0...1]
     */
    public LightHSLSetMessage(long dstAddress, Node node, App app, float[] hsl) {
        this(dstAddress, node, app, hsl, false);
    }

    /**
     * @param hsl h: [0 .. 360), s: [0...1], l: [0...1]
     */
    public LightHSLSetMessage(long dstAddress, Node node, App app, float[] hsl, boolean unacknowledged) {
        super(dstAddress, node, app);

        mHSL = hsl;

        mUnacknowledged = unacknowledged;
    }

    @Override
    public byte[] getOpCode() {
        if (mUnacknowledged) {
            return new byte[]{(byte) 0x82, 0x77};
        } else {
            return new byte[]{(byte) 0x82, 0x76};
        }

    }

    @Override
    public byte[] getParameters() {
        int[] lightHSL = MeshUtils.HSLtoLightMeshHSL(mHSL);
        int lightLightness = lightHSL[2];
        int lightHue = lightHSL[0];
        int lightSaturation = lightHSL[1];
        byte tid = (byte) System.nanoTime();
        return new byte[]{
                (byte) (lightLightness & 0xff), (byte) ((lightLightness >> 8) & 0xff),
                (byte) (lightHue & 0xff), (byte) ((lightHue >> 8) & 0xff),
                (byte) (lightSaturation & 0xff), (byte) ((lightSaturation >> 8) & 0xff),
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
