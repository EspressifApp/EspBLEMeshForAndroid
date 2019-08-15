package com.espressif.blemesh.model.message.standard;

import androidx.core.graphics.ColorUtils;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;
import com.espressif.blemesh.utils.MeshUtils;

public class LightHSLSetMessage extends Message {
    private float[] mHSL;

    public LightHSLSetMessage(long dstAddress, Node node, App app, float[] hsl) {
        super(dstAddress, node, app);

        mHSL = hsl;
    }

    public LightHSLSetMessage(long dstAddress, Node node, App app, int color) {
        super(dstAddress, node, app);

        mHSL = new float[3];
        ColorUtils.colorToHSL(color, mHSL);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x82, 0x76};
    }

    @Override
    public byte[] getParameters() {
        int[] lightHSL = MeshUtils.HSLtoLightHSL(mHSL);
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
