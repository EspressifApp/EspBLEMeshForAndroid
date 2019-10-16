package com.espressif.blemesh.model.message.custom;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;
import com.espressif.blemesh.utils.MeshUtils;

/**
 * New Bin Version Notification
 */
public class OtaNBVNMessage extends MeshMessage {
    private byte[] mManufacturerID;
    private byte[] mBinID;
    private byte[] mVersion;
    private boolean mClearFlash = false;

    public OtaNBVNMessage(Node node, byte[] binID) {
        super(node.getUnicastAddress(), node);

        mBinID = binID;
        byte[] appKey = {
                0x02, (byte) 0xE5, (byte) 0xE5, 0x02,
                0x02, (byte) 0xE5, (byte) 0xE5, 0x02,
                0x02, (byte) 0xE5, (byte) 0xE5, 0x02,
                0x02, (byte) 0xE5, mBinID[0], mBinID[1],
        };
        long appIndex = MeshConstants.ADDRESS_UNICAST_MAX - MeshUtils.bigEndianBytesToLong(mBinID);
        App app = new App(appKey, appIndex);
        setApp(app);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0xCA, mManufacturerID[0], mManufacturerID[1]};
    }

    @Override
    public byte[] getParameters() {
        return new byte[]{
                mManufacturerID[0],
                mManufacturerID[1],
                mBinID[0],
                mBinID[1],
                mVersion[0],
                mVersion[1],
                (byte) (mClearFlash ? 1 : 0),
                (byte) (MeshConstants.ADDRESS_OTA_GROUP & 0xff),
                (byte) ((MeshConstants.ADDRESS_OTA_GROUP >> 8) & 0xff)
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

    public void setVersion(byte[] version) {
        mVersion = version;
    }

    public void setClearFlash(boolean clearFlash) {
        mClearFlash = clearFlash;
    }
}
