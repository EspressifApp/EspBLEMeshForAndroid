package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;
import com.espressif.blemesh.utils.MeshUtils;

import libs.espressif.utils.DataUtil;

public class ModelSubscriptionDeleteMessage extends MeshMessage {
    private Model mModel;
    private long mSubAddress;

    public ModelSubscriptionDeleteMessage(Node node, Model model, long subAddr) {
        super(node.getUnicastAddress(), node);

        mModel = model;
        mSubAddress = subAddr;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x80, 0x1C};
    }

    @Override
    public byte[] getParameters() {
        byte[] elementAddr = MeshUtils.addressLongToLittleEndianBytes(mModel.getElementAddress());
        byte[] subAddress = MeshUtils.addressLongToLittleEndianBytes(mSubAddress);
        byte[] modelID = DataUtil.hexStringToLittleEndianBytes(mModel.getId());
        return DataUtil.mergeBytes(elementAddr, subAddress, modelID);
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
