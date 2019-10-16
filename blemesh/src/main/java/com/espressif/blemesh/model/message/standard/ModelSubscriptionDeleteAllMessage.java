package com.espressif.blemesh.model.message.standard;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.MeshMessage;
import com.espressif.blemesh.utils.MeshUtils;

import libs.espressif.utils.DataUtil;

public class ModelSubscriptionDeleteAllMessage extends MeshMessage {
    private  Model mModel;

    public ModelSubscriptionDeleteAllMessage(Node node, Model model) {
        super(node.getUnicastAddress(), node);

        mModel = model;
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0x80, 0x1D};
    }

    @Override
    public byte[] getParameters() {
        byte[] elementAddr = MeshUtils.addressLongToLittleEndianBytes(mModel.getElementAddress());
        byte[] modelID = DataUtil.hexStringToLittleEndianBytes(mModel.getId());
        return DataUtil.mergeBytes(elementAddr, modelID);
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
