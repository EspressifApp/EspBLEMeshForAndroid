package com.espressif.blemesh.model.message.custom;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import libs.espressif.utils.DataUtil;

public class FastGroupBindMessage extends Message {
    private long mGroupAddr;
    private List<Long> mNodeAddrList;

    public FastGroupBindMessage(long dstAddress, Node node, App app) {
        super(dstAddress, node, app);

        mNodeAddrList = new ArrayList<>();
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0xC8, (byte) 0xE5, 0x02};
    }

    @Override
    public byte[] getParameters() {
        // Group Address
        byte[] parameters = {(byte) (mGroupAddr & 0xff), (byte) ((mGroupAddr >> 8) & 0xff)};
        // Nodes Address
        for (long addr : mNodeAddrList) {
            byte[] nodeAddr = {(byte) (addr & 0xff), (byte) ((addr >> 8) & 0xff)};
            parameters = DataUtil.mergeBytes(parameters, nodeAddr);
        }
        return parameters;
    }

    @Override
    public int getProxyType() {
        return MeshConstants.PROXY_TYPE_NETWORK_PDU;
    }

    @Override
    public SecurityKey getSecurityKey() {
        return SecurityKey.AppKey;
    }

    public void setGroupAddr(long groupAddr) {
        mGroupAddr = groupAddr;
    }

    public void setNodeAddrList(Collection<Long> nodeAddrList) {
        mNodeAddrList.clear();
        mNodeAddrList.addAll(nodeAddrList);
    }
}
