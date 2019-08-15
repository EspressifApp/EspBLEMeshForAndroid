package com.espressif.blemesh.model.message.custom;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;
import com.espressif.blemesh.utils.MeshUtils;

import java.util.Arrays;

import libs.espressif.utils.DataUtil;

public class FastProvInfoSetMessage extends Message {
    private Integer mProvCount;
    private Long mUnicastAddressMin;
    private Long mUnicastAddressMax;
    private Integer mFlags;
    private Long mIvIndex;
    private Long mNetKeyIndex;
    private Long mGroupAddress;
    private Long mPrimaryProvisionerAddress;
    private byte[] mMatchValue;
    private Integer mAction;

    public FastProvInfoSetMessage(Node node, App app) {
        super(node.getUnicastAddress(), node, app);
    }

    @Override
    public byte[] getOpCode() {
        return new byte[]{(byte) 0xC0, (byte) 0xE5, 0x02};
    }

    @Override
    public byte[] getParameters() {
        int context0 = 0;
        int context1 = 0;
        byte[] parameters = new byte[0];
        if (mProvCount != null && mProvCount >= 0) {
            context0 = 1;
            parameters = new byte[]{(byte) (mProvCount & 0xff), (byte) ((mProvCount >> 8) & 0xff)};
        }
        if (mUnicastAddressMin != null) {
            context0 = context0 | (1 << 1);
            byte[] unicastAddrMinBytes = MeshUtils.longToLittleEndianBytes(mUnicastAddressMin, 2);
            parameters = DataUtil.mergeBytes(parameters, unicastAddrMinBytes);
        }
        if (mUnicastAddressMax != null) {
            context0 = context0 | (1 << 2);
            byte[] unicastAddrMaxBytes = MeshUtils.longToLittleEndianBytes(mUnicastAddressMax, 2);
            parameters = DataUtil.mergeBytes(parameters, unicastAddrMaxBytes);
        }
        if (mFlags != null) {
            context0 = context0 | (1 << 3);
            byte[] flagBytes = {(byte)(mFlags & 0xff)};
            parameters = DataUtil.mergeBytes(parameters, flagBytes);
        }
        if (mIvIndex != null) {
            context0 = context0 | (1 << 4);
            byte[] ivIndexBytes = MeshUtils.longToLittleEndianBytes(mIvIndex, 4);
            parameters = DataUtil.mergeBytes(parameters, ivIndexBytes);
        }
        if (mNetKeyIndex != null) {
            context0 = context0 | (1 << 5);
            byte[] netKeyIndexBytes = MeshUtils.longToLittleEndianBytes(mNetKeyIndex, 2);
            parameters = DataUtil.mergeBytes(parameters, netKeyIndexBytes);
        }
        if (mGroupAddress != null) {
            context0 = context0 | (1 << 6);
            byte[] groupAddrBytes = MeshUtils.longToLittleEndianBytes(mGroupAddress, 2);
            parameters = DataUtil.mergeBytes(parameters, groupAddrBytes);
        }
        if (mPrimaryProvisionerAddress != null) {
            context0 = context0 | (1 << 7);
            byte[] primaryProvisionerAddrBytes = MeshUtils.longToLittleEndianBytes(mPrimaryProvisionerAddress, 2);
            parameters= DataUtil.mergeBytes(parameters, primaryProvisionerAddrBytes);
        }
        if (mMatchValue != null && mMatchValue.length > 0) {
            context1 = 1;
            byte[] matchValueBytes = Arrays.copyOf(mMatchValue, mMatchValue.length); // DataUtil.reverseBytes(mMatchValue);
            System.out.println("DEVICE UUID = " + DataUtil.bigEndianBytesToHexString(matchValueBytes));
            parameters = DataUtil.mergeBytes(parameters, matchValueBytes);
        }
        if (mAction != null) {
            context1 = context1 | (1 << 1);
            byte[] actionBytes = {(byte)(int)mAction};
            parameters = DataUtil.mergeBytes(parameters, actionBytes);
        }

        byte[] context = {(byte) context0, (byte) context1};

        return DataUtil.mergeBytes(context, parameters);
    }

    @Override
    public int getProxyType() {
        return MeshConstants.PROXY_TYPE_NETWORK_PDU;
    }

    @Override
    public SecurityKey getSecurityKey() {
        return SecurityKey.AppKey;
    }

    public void setProvCount(Integer provCount) {
        mProvCount = provCount;
    }

    public Integer getProvCount() {
        return mProvCount;
    }

    public void setUnicastAddressMin(Long unicastAddressMin) {
        mUnicastAddressMin = unicastAddressMin;
    }

    public Long getUnicastAddressMin() {
        return mUnicastAddressMin;
    }

    public void setUnicastAddressMax(Long unicastAddressMax) {
        mUnicastAddressMax = unicastAddressMax;
    }

    public Long getUnicastAddressMax() {
        return mUnicastAddressMax;
    }

    public void setFlags(Integer flags) {
        mFlags = flags;
    }

    public Integer getFlags() {
        return mFlags;
    }

    public void setIvIndex(Long ivIndex) {
        mIvIndex = ivIndex;
    }

    public Long getIvIndex() {
        return mIvIndex;
    }

    public void setNetKeyIndex(Long netKeyIndex) {
        mNetKeyIndex = netKeyIndex;
    }

    public Long getNetKeyIndex() {
        return mNetKeyIndex;
    }

    public void setGroupAddress(Long groupAddress) {
        mGroupAddress = groupAddress;
    }

    public Long getGroupAddress() {
        return mGroupAddress;
    }

    public void setPrimaryProvisionerAddress(Long primaryProvisionerAddress) {
        mPrimaryProvisionerAddress = primaryProvisionerAddress;
    }

    public Long getPrimaryProvisionerAddress() {
        return mPrimaryProvisionerAddress;
    }

    public void setMatchValue(byte[] matchValue) {
        mMatchValue = matchValue;
    }

    public byte[] getMatchValue() {
        return mMatchValue;
    }

    public void setAction(Integer action) {
        mAction = action;
    }

    public Integer getAction() {
        return mAction;
    }
}
