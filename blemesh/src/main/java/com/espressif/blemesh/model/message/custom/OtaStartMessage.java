package com.espressif.blemesh.model.message.custom;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;
import com.espressif.blemesh.utils.MeshUtils;

public class OtaStartMessage extends MeshMessage {
    private byte[] mManufacturerID;
    private byte[] mSoftApSSID;
    private byte[] mSoftApPassword;

    public OtaStartMessage(Node node, byte[] binID) {
        super(node.getUnicastAddress(), node);

        byte[] appKey = {
                0x02, (byte) 0xE5, (byte) 0xE5, 0x02,
                0x02, (byte) 0xE5, (byte) 0xE5, 0x02,
                0x02, (byte) 0xE5, (byte) 0xE5, 0x02,
                0x02, (byte) 0xE5, binID[0], binID[1],
        };
        long appIndex = MeshConstants.ADDRESS_UNICAST_MAX - MeshUtils.bigEndianBytesToLong(binID);
        App app = new App(appKey, appIndex);
        setApp(app);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0xCC, mManufacturerID[0], mManufacturerID[1]};
    }

    @Override
    public byte[] getParameters() {
        return new byte[] {
                0x00,
                mSoftApSSID[1], mSoftApSSID[0],
                mSoftApPassword[1], mSoftApPassword[0]
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

    public void setManufacturerID(byte[] manufacturerID) {
        mManufacturerID = manufacturerID;
    }

    public void setSoftApSSID(byte[] softApSSID) {
        if (softApSSID.length != 2) {
            throw new IllegalArgumentException("SSID data length must be 2");
        }
        mSoftApSSID = softApSSID;
    }

    public void setSoftApPassword(byte[] softApPassword) {
        if (softApPassword.length != 2) {
            throw new IllegalArgumentException("Password data length must be 2");
        }
        mSoftApPassword = softApPassword;
    }


}
