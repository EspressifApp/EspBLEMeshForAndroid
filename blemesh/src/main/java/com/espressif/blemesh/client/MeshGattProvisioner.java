package com.espressif.blemesh.client;

import android.util.Log;

import com.espressif.blemesh.client.abs.MeshCommunicationClient;
import com.espressif.blemesh.client.callback.ProvisioningCallback;
import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.utils.MeshAlgorithmUtils;
import com.espressif.blemesh.utils.MeshUtils;

import org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.LinkedList;
import java.util.List;

import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

import static android.content.ContentValues.TAG;

public class MeshGattProvisioner extends MeshCommunicationClient implements IMeshProvisioner{
    private static final int HEAD_OFFSET = 2;

    private EspLog mLog = new EspLog(getClass());

    private ProvisioningCallback mProvisioningCallback;

    private int mProxyType = MeshConstants.PROXY_TYPE_PROVISIONING_PDU;

    private int mProvisioningType;
    private LinkedList<byte[]> mPostSplits = new LinkedList<>();
    private LinkedList<byte[]> mRecvSplits = new LinkedList<>();

    private BigInteger mECPublicKeyX;
    private BigInteger mECPublicKeyY;
    private BigInteger mECPrivateKey;
    private byte[] mDHSecret;

    private byte[] mInviteData;
    private byte[] mCapabilitiesData;
    private byte[] mStartData;
    private byte[] mAppPublicKeyData;
    private byte[] mDevPublicKeyData;

    private byte[] mSaltData;
    private byte[] mAppRandomData;
    private byte[] mDevConfirmationData;
    private byte[] mDevRandomData;

    private int mElementNum;
    private long mUnicastAddr;

    private String mNodeName;
    private Network mNetwork;

    @Override
    public void release() {
        super.release();

        mProvisioningCallback = null;

        mECPublicKeyX = null;
        mECPublicKeyY = null;
        mECPrivateKey = null;
        mDHSecret = null;

        mInviteData = null;
        mCapabilitiesData = null;
        mStartData = null;
        mAppPublicKeyData = null;
        mDevPublicKeyData = null;

        mSaltData = null;
        mAppRandomData = null;
        mDevConfirmationData = null;
        mDevRandomData = null;

        mNodeName = null;
        mNetwork = null;
    }

    @Override
    public void setProvisioningCallback(ProvisioningCallback provisioningCallback) {
        mProvisioningCallback = provisioningCallback;
    }

    @Override
    public void onNotification(byte[] data) {
        int[] proxy = MeshUtils.getProxySarAndType(data[0]);
        switch (proxy[0]) {
            case MeshConstants.PROXY_SAR_COMPLETE:
                break;
            case MeshConstants.PROXY_SAR_FIRST:
                mRecvSplits.clear();
                mRecvSplits.add(data);
                return;
            case MeshConstants.PROXY_SAR_CONTINUATION:
                mRecvSplits.add(DataUtil.subBytes(data, 1));
                return;
            case MeshConstants.PROXY_SAR_LAST:
                mRecvSplits.add(DataUtil.subBytes(data, 1));

                data = new byte[0];
                for (byte[] bytes : mRecvSplits) {
                    data = DataUtil.mergeBytes(data, bytes);
                }
                mRecvSplits.clear();
                break;
            default:
                return;
        }

        int provisioningType = data[1] & 0xff;
        switch (provisioningType) {
            case MeshConstants.PROVISIONING_TYPE_CAPABILITIES:
                parseCapabilities(data);
                break;
            case MeshConstants.PROVISIONING_TYPE_PUBLIC_KEY:
                parseDevicePublicKey(data);
                break;
            case MeshConstants.PROVISIONING_TYPE_CONFIRMATION:
                parseDeviceConfirmation(data);
                break;
            case MeshConstants.PROVISIONING_TYPE_RANDOM:
                parseDeviceRandom(data);
                break;
            case MeshConstants.PROVISIONING_TYPE_COMPLETE:
                parseDeviceComplete(data);
                break;
            case MeshConstants.PROVISIONING_TYPE_FAILED:
                parseDeviceFailed(data);
                break;
        }
    }

    @Override
    public void onWrote(byte[] data) {
        mLog.d("onWrote() " + DataUtil.bigEndianBytesToHexString(data));
        int[] proxy = MeshUtils.getProxySarAndType(data[0]);
        switch (proxy[0]) {
            case MeshConstants.PROXY_SAR_COMPLETE:
            case MeshConstants.PROXY_SAR_FIRST:
                mProvisioningType = data[1] & 0xff;
                break;
        }

        if (!mPostSplits.isEmpty()) {
            byte[] split = mPostSplits.poll();
            int sar = mPostSplits.isEmpty() ?
                    MeshConstants.PROXY_SAR_LAST : MeshConstants.PROXY_SAR_CONTINUATION;
            byte[] writeData = MeshUtils.getData(sar, mProxyType, split);
            getWriteChar().setValue(writeData);
            getGatt().writeCharacteristic(getWriteChar());
            return;
        }

        switch (mProvisioningType) {
            case MeshConstants.PROVISIONING_TYPE_INVITE:
                mLog.d("Write PROVISIONING_TYPE_INVITE success");
                break;
            case MeshConstants.PROVISIONING_TYPE_START:
                mLog.d("Write PROVISIONING_TYPE_START success");
                postProvisionerPublicKey();
                break;
            case MeshConstants.PROVISIONING_TYPE_PUBLIC_KEY:
                mLog.d("Write PROVISIONING_TYPE_PUBLIC_KEY success");
                break;
            case MeshConstants.PROVISIONING_TYPE_INPUT_COMPLETE:
                mLog.d("Write PROVISIONING_TYPE_INPUT_COMPLETE success");
                break;
            case MeshConstants.PROVISIONING_TYPE_CONFIRMATION:
                mLog.d("Write PROVISIONING_TYPE_CONFIRMATION success");
                break;
            case MeshConstants.PROVISIONING_TYPE_RANDOM:
                mLog.d("Write PROVISIONING_TYPE_RANDOM success");
                break;
            case MeshConstants.PROVISIONING_TYPE_DATA:
                mLog.d("Write PROVISIONING_TYPE_DATA success");
                break;
        }
    }

    private static byte[] getProvisioningPDU(int proxySar, int proxyType, int provisioningType, byte[] provisioningField) {
        return MeshUtils.getData(proxySar, proxyType, new byte[]{(byte) provisioningType}, provisioningField);
    }

    @Override
    public void provisioning(String deviceName, Network network) {
        mNodeName = deviceName;
        mNetwork = network;

        initEcc();

        MeshObjectBox.getInstance().deleteNode(getDevice().getAddress());;

        invite();
    }

    private void initEcc() {
        KeyPairGenerator generator = new KeyPairGenerator.EC();
        generator.initialize(256);
        KeyPair pair = generator.genKeyPair();
        ECPublicKey publicKey = (ECPublicKey) pair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) pair.getPrivate();

        mECPublicKeyX = publicKey.getW().getAffineX();
        mECPublicKeyY = publicKey.getW().getAffineY();
        mECPrivateKey = privateKey.getS();
    }

    private void writeGatt(int type, byte[] field) {
        if (field.length + HEAD_OFFSET <= getAvailableGattMTU()) {
            byte[] data = getProvisioningPDU(MeshConstants.PROXY_SAR_COMPLETE, mProxyType,
                    type, field);

            getWriteChar().setValue(data);
            getGatt().writeCharacteristic(getWriteChar());
        } else {
            int splitLengthLimit = getAvailableGattMTU() - 1;
            field = DataUtil.mergeBytes(new byte[]{(byte) type}, field);
            List<byte[]> splits = DataUtil.splitBytes(field, splitLengthLimit);
            mPostSplits.addAll(splits);

            byte[] data = MeshUtils.getData(MeshConstants.PROXY_SAR_FIRST, mProxyType, mPostSplits.poll());
            getWriteChar().setValue(data);
            getGatt().writeCharacteristic(getWriteChar());
        }
    }

    private void invite() {
        byte[] field = new byte[]{0x00};
        mInviteData = field;

        writeGatt(MeshConstants.PROVISIONING_TYPE_INVITE, field);
    }

    private void parseCapabilities(byte[] data) {
        mCapabilitiesData = DataUtil.subBytes(data, HEAD_OFFSET);
        mElementNum = mCapabilitiesData[0] & 0xff;
        long[] addrArray = MeshUtils.generateContinuousUnicastAddress(mElementNum);
        if (addrArray == null) {
            mUnicastAddr = MeshUtils.generateUnicastAddress();
        } else {
            mUnicastAddr = addrArray[0];
        }

        start();
    }

    private void start() {
        byte algorithms = 0x00;
        byte publicKey = 0x00;
        byte authenticationMethod = 0x00;
        byte authenticationAction = 0x00;
        byte authenticationSize = 0x00;
        byte[] field = new byte[]{
                algorithms,
                publicKey,
                authenticationMethod,
                authenticationAction,
                authenticationSize
        };
        mStartData = field;

        writeGatt(MeshConstants.PROVISIONING_TYPE_START, field);
    }

    private void postProvisionerPublicKey() {
        byte[] publicXBytes = DataUtil.hexStringToBigEndianBytes(mECPublicKeyX.toString(16));
        byte[] publicYBytes = DataUtil.hexStringToBigEndianBytes(mECPublicKeyY.toString(16));
        byte[] publicKeyBytes = DataUtil.mergeBytes(publicXBytes, publicYBytes);
        mLog.d("PublicKey = " + DataUtil.bigEndianBytesToHexString(publicKeyBytes));
        mAppPublicKeyData = publicKeyBytes;

        writeGatt(MeshConstants.PROVISIONING_TYPE_PUBLIC_KEY, publicKeyBytes);
    }

    private void parseDevicePublicKey(byte[] data) {
        int length = 32;
        byte[] deviceXData = new byte[length];
        byte[] deviceYData = new byte[length];
        System.arraycopy(data, HEAD_OFFSET, deviceXData, 0, length);
        System.arraycopy(data, HEAD_OFFSET + length, deviceYData, 0, length);
        BigInteger deviceX = new BigInteger(DataUtil.bigEndianBytesToHexString(deviceXData), 16);
        BigInteger deviceY = new BigInteger(DataUtil.bigEndianBytesToHexString(deviceYData), 16);

        BigInteger ecdhSecret = MeshAlgorithmUtils.generateECDHSecret(mECPrivateKey, deviceX, deviceY);
        assert ecdhSecret != null;
        mDHSecret = DataUtil.hexStringToBigEndianBytes(ecdhSecret.toString(16));
        mDevPublicKeyData = DataUtil.mergeBytes(deviceXData, deviceYData);

        postProvisonorConfirmation();
    }

    private void inputComplete() {
        byte[] data = getProvisioningPDU(MeshConstants.PROXY_SAR_COMPLETE, mProxyType,
                MeshConstants.PROVISIONING_TYPE_INPUT_COMPLETE, null);
        getWriteChar().setValue(data);
        getGatt().writeCharacteristic(getWriteChar());
    }

    private byte[] getConfirmationData(byte[] random, byte[] authValue) {
        byte[] key = MeshAlgorithmUtils.k1(mDHSecret, mSaltData, MeshConstants.BYTES_PRCK);
        byte[] random_authValue = DataUtil.mergeBytes(random, authValue);
        return MeshAlgorithmUtils.AES_CMAC(key, random_authValue);
    }

    private void postProvisonorConfirmation() {
        byte[] input = DataUtil.mergeBytes(mInviteData, mCapabilitiesData, mStartData, mAppPublicKeyData,
                mDevPublicKeyData);
        mSaltData = MeshAlgorithmUtils.s1(input);

        mAppRandomData = MeshUtils.generateRandom();

        byte[] confirmation = getConfirmationData(mAppRandomData, MeshConstants.AUTH_VALUE_ZERO);

        writeGatt(MeshConstants.PROVISIONING_TYPE_CONFIRMATION, confirmation);
    }

    private void parseDeviceConfirmation(byte[] data) {
        mLog.d("parseDeviceConfirmation");
        byte[] confirmation = new byte[data.length - HEAD_OFFSET];
        System.arraycopy(data, HEAD_OFFSET, confirmation, 0, confirmation.length);
        mDevConfirmationData = confirmation;

        postProvisionorRandom();
    }

    private void postProvisionorRandom() {
        mLog.d("postProvisionorRandom");
        writeGatt(MeshConstants.PROVISIONING_TYPE_RANDOM, mAppRandomData);
    }

    private void parseDeviceRandom(byte[] data) {
        mLog.d("parseDeviceRandom");
        byte[] random = new byte[data.length - HEAD_OFFSET];
        System.arraycopy(data, HEAD_OFFSET, random, 0, random.length);
        byte[] confirmation = getConfirmationData(random, MeshConstants.AUTH_VALUE_ZERO);

        boolean valid = true;
        if (confirmation.length == mDevConfirmationData.length) {
            for (int i = 0; i < confirmation.length; i++) {
                if (confirmation[i] != mDevConfirmationData[i]) {
                    valid = false;
                    break;
                }
            }
        }

        mLog.d("Confirmation valid is " + valid);
        if (valid) {
            mDevRandomData = random;
            postData();
        } else {
            if (mProvisioningCallback != null) {
                mProvisioningCallback.onProvisioningFailed(ProvisioningCallback.CODE_FAILED);
            }
            getGatt().disconnect();
        }
    }

    private void postData() {
        byte[] salt = MeshAlgorithmUtils.s1(DataUtil.mergeBytes(mSaltData, mAppRandomData, mDevRandomData));
        byte[] key = MeshAlgorithmUtils.k1(mDHSecret, salt, MeshConstants.BYTES_PRSK);
        byte[] nonceFull = MeshAlgorithmUtils.k1(mDHSecret, salt, MeshConstants.BYTES_PRSN);
        byte[] nonce = new byte[13];
        System.arraycopy(nonceFull, 3, nonce, 0, nonce.length);

        byte[] netKeyIndexBytes = MeshUtils.getIndexBytesWithOneIndex(mNetwork.getKeyIndex());
        netKeyIndexBytes = DataUtil.reverseBytes(netKeyIndexBytes);
        byte[] unicastAddr = MeshUtils.addressLongToBigEndianBytes(mUnicastAddr);
        byte[] config = DataUtil.mergeBytes(mNetwork.getNetKey(),
                netKeyIndexBytes,
                new byte[]{0x00},
                mNetwork.getIVIndexBytes(),
                unicastAddr);

        byte[] encrypt = MeshAlgorithmUtils.AES_CCM_Encrypt(key, nonce, 64, config);

        writeGatt(MeshConstants.PROVISIONING_TYPE_DATA, encrypt);
    }

    private void parseDeviceComplete(byte[] data) {
        Log.d(TAG, "parseDeviceComplete");
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        byte[] devKey = getDeviceKey();
        String devKeyStr = DataUtil.bigEndianBytesToHexString(devKey);
        String uuidStr = DataUtil.bigEndianBytesToHexString(getDeviceUUID());
        // Save node db
        dbManager.saveNode(getDevice().getAddress(), uuidStr, devKeyStr, mNodeName, mUnicastAddr, mElementNum,
                mNetwork.getKeyIndex());

        if (mProvisioningCallback != null) {
            Node node = new Node();
            node.setMac(getDevice().getAddress());
            node.setUUID(uuidStr);
            node.setName(mNodeName);
            node.setDeviceKey(devKey);
            node.setUnicastAddress(mUnicastAddr);
            node.setElementCount(mElementNum);
            mProvisioningCallback.onProvisioningSuccess(ProvisioningCallback.CODE_SUCCESS, node);
        }

//        getGatt().disconnect();
    }

    private void parseDeviceFailed(byte[] data) {
        mLog.w("parseDeviceFailed");
        if (mProvisioningCallback != null) {
            int errCode = data[HEAD_OFFSET] & 0xff;
            mProvisioningCallback.onProvisioningFailed(errCode);
        }

        getGatt().disconnect();
    }

    private byte[] getDeviceKey() {
        byte[] provisioningSalt = MeshAlgorithmUtils.s1(DataUtil.mergeBytes(mSaltData, mAppRandomData, mDevRandomData));
        return MeshAlgorithmUtils.k1(mDHSecret, provisioningSalt, MeshConstants.BYTES_PRDK);
    }
}
