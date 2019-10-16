package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;
import com.espressif.blemesh.utils.MeshUtils;

import libs.espressif.utils.DataUtil;

public class ModelAppBindMessage extends MeshMessage {
    private Model mModel;
    private long mAppKeyIndex;

    public ModelAppBindMessage(Node node, Model model, long appKeyIndex) {
        super(node.getUnicastAddress(), node);

        mModel = model;
        mAppKeyIndex = appKeyIndex;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x80, 0x3D};
    }

    @Override
    public byte[] getParameters() {
        byte[] elementAddress = MeshUtils.addressLongToLittleEndianBytes(mModel.getElementAddress());
        byte[] appIndexBytes = MeshUtils.getIndexBytesWithOneIndex(mAppKeyIndex);
        byte[] modelId = DataUtil.hexStringToLittleEndianBytes(mModel.getId());
        return DataUtil.mergeBytes(elementAddress, appIndexBytes, modelId);
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
