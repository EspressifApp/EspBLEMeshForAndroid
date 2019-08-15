package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;
import com.espressif.blemesh.utils.MeshUtils;

import libs.espressif.utils.DataUtil;

public class AppKeyAddMessage extends Message {
    private byte[] mAppKey;
    private long mAppKeyIndex;
    private long mNetKeyIndex;

    public AppKeyAddMessage(Node node, byte[] appKey, long appKeyIndex, long netKeyIndex) {
        super(node.getUnicastAddress(), node);

        mAppKey = appKey;
        mAppKeyIndex = appKeyIndex;
        mNetKeyIndex = netKeyIndex;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{0x00};
    }

    @Override
    public byte[] getParameters() {
        byte[] indexes = MeshUtils.getIndexBytesWithTwoIndexes(mNetKeyIndex, mAppKeyIndex);
        return DataUtil.mergeBytes(indexes, mAppKey);
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
