package com.espressif.blemesh.client.abs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import libs.espressif.ble.EspBleUtils;
import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

import static android.content.ContentValues.TAG;

public class AdvProvisioner implements PrivateProvisioner {
    private static final long POST_ACK_TIMEOUT = 2000;

    private EspLog mLog = new EspLog(getClass());

    private volatile boolean mClosed = false;

    private ScanResult mScanResult;
    private BluetoothDevice mDevice;
    private ScanRecord mScanRecord;
    private byte[] mDeviceUUID;

    private final Object mAdvLock = new Object();
    private BluetoothLeAdvertiser mAdvertiser;
    private Set<AdvertiseCallback> mAdvertiseCallbacks;

    private BluetoothLeScanner mScanner;
    private ScanCallback mScanCallback;

    private PrivateProvisioningCallback mMeshCallback;

    private byte[] mLinkID;
    private int mAppTransactionNumber = 0x00;
    private int mDevTransactionNumber = 0x80;

    private byte mInviteAckTN;
    private byte mStartAckTN;
    private byte mAppPublicKeyAckTN;
    private byte mProvisonorConfirmationTN;
    private byte mProvisionorRandomTN;
    private byte mProvisionorDataTN;

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

    private final Object mTransactionLock = new Object();
    private Map<Byte, TransactionPDU> mTransactionPDUMap;
//    private CRC8 mCRC8;

    private ExecutorService mExecutorService;

    public AdvProvisioner(ScanResult scanResult) {
        mScanResult = scanResult;
        mDevice = scanResult.getDevice();
        mScanRecord = scanResult.getScanRecord();
        assert mScanRecord != null;
        mDeviceUUID = DataUtil.subBytes(mScanRecord.getBytes(), 11, 16);

        mAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        mAdvertiseCallbacks = new HashSet<>();

        mScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        mScanCallback = new LeScanCallback();

        mTransactionPDUMap = new Hashtable<>();
//        mCRC8 = new CRC8();
        mExecutorService = Executors.newCachedThreadPool();
    }

    public void setProvisioningCallback(PrivateProvisioningCallback callback) {
        mMeshCallback = callback;
    }

    public void close() {
        mClosed = true;

        stopAdv();
        stopScanLe();

        mExecutorService.shutdownNow();
        try {
            mExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private int calcCRC8(byte[] data) {
//        mCRC8.reset();
//        mCRC8.update(data);
//        return (int) mCRC8.getValue();
        // TODO
        return 0;
    }

    private void startScanLe() {
        if (mClosed) {
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceAddress(mDevice.getAddress())
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);
        mScanner.startScan(filters, settings, mScanCallback);
    }

    private void stopScanLe() {
        mScanner.stopScan(mScanCallback);
    }

    private void startAdv(byte[]... bytesArray) {
        if (mClosed) {
            return;
        }

        synchronized (mAdvLock) {
            for (byte[] data : bytesArray) {
                UUID serviceUUID = EspBleUtils.newUUID(new byte[]{data[1], data[0]});
                byte[] serviceData = DataUtil.subBytes(data, 2);
                AdvertiseData advertiseData = new AdvertiseData.Builder()
                        .addServiceData(new ParcelUuid(serviceUUID), serviceData)
                        .setIncludeDeviceName(false)
                        .build();
                AdvertiseSettings settings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(false)
                        .build();
                AdvertiseCallback callback = new LeAdvCallback();
                mAdvertiseCallbacks.add(callback);

                mAdvertiser.startAdvertising(settings, advertiseData, callback);
            }
        }

    }

    private void stopAdv(AdvertiseCallback callback) {
        synchronized (mAdvLock) {
            mAdvertiser.stopAdvertising(callback);
            mAdvertiseCallbacks.remove(callback);
        }
    }

    private void stopAdv() {
        synchronized (mAdvLock) {
            for (AdvertiseCallback callback : mAdvertiseCallbacks) {
                mAdvertiser.stopAdvertising(callback);
            }
            mAdvertiseCallbacks.clear();
        }
    }

    private void advProvisioningPDU(byte transNum, int type, byte[] field) {
        byte[] provisioningData = getProvisioningData(type, field);
        int fcs = calcCRC8(provisioningData);

        List<byte[]> dataList = new LinkedList<>();
        final int segLenLimit = 20;
        int offset = 0;
        while (true) {
            if (offset + segLenLimit < provisioningData.length) {
                byte[] data = DataUtil.subBytes(provisioningData, offset, segLenLimit);
                dataList.add(data);
                offset += segLenLimit;
            } else {
                byte[] data = DataUtil.subBytes(provisioningData, offset);
                dataList.add(data);
                break;
            }
        }

        int segN = dataList.size() - 1;
        byte[][] dataArray = new byte[dataList.size()][];
        for (int i = 0; i < dataArray.length; i++) {
            byte[] data = dataList.get(i);
            if (i == 0) {
                byte[] startSegment = getTransactionStartPDU(segN, provisioningData.length, fcs, data);
                dataArray[i] = getAdvData(mLinkID, transNum, startSegment);
            } else {
                byte[] segment = getTransacionContinuationData(i, data);
                dataArray[i] = getAdvData(mLinkID, transNum, segment);
            }
        }

        startAdv(dataArray);
    }

    private byte[] getProvisioningData(int type, byte[] field) {
        return DataUtil.mergeBytes(new byte[]{(byte) type}, field);
    }

    private byte[] getTransactionStartPDU(int segN, int totalLen, int fcs, byte[] data) {
        byte firstByte = (byte) (((segN << 2) & 0xff));
        byte totalLen1 = (byte) ((totalLen >> 8) & 0xff);
        byte totalLen2 = (byte) (totalLen & 0xff);
        byte[] header = {firstByte, totalLen1, totalLen2, (byte) fcs};
        return DataUtil.mergeBytes(header, data);
    }

    private byte[] getTransacionContinuationData(int segmentIndex, byte[] data) {
        byte firstByte = (byte) (((segmentIndex << 2) & 0xff) | MeshConstants.GPCF_TRANSACTION_CONTINUATION);
        byte[] header = {firstByte};
        return DataUtil.mergeBytes(header, data);
    }

    private void advAck(byte transNum) {
        byte[] data = getAdvData(mLinkID, transNum, getTransactionAckPDU());
        startAdv(data);
    }

    private byte[] getTransactionAckPDU() {
        return new byte[]{0x01};
    }

    private byte[] getAdvData(byte[] linkID, byte transNum, byte[] provisioningPDU) {
        return DataUtil.mergeBytes(linkID, new byte[]{transNum}, provisioningPDU);
    }

    private byte appTransNumGetAndIncreament() {
        int result = mAppTransactionNumber;
        mAppTransactionNumber = (mAppTransactionNumber + 1) & 0x7f;

        return (byte) result;
    }

    private int getDevTransNum(int num) {
        if (num > 0xff) {
            num = 0x80;
        }

        return num;
    }

    private boolean checkLinkID(byte[] linkID) {
        if (mLinkID.length != linkID.length) {
            return false;
        }

        for (int i = 0; i < mLinkID.length; i++) {
            if (mLinkID[i] != linkID[i]) {
                return false;
            }
        }
        return true;
    }

    private void parse(int advType, byte devTransNum, byte[] provisioningPDU) {
        mDevTransactionNumber = devTransNum;

        int header0 = provisioningPDU[0] & 0xff;
        int gpcf = header0 & 3;
        switch (gpcf) {
            case MeshConstants.GPCF_TRANSACTION_START: {
                // Transaction Start PDU
                parseTransactionStartPDU(devTransNum, provisioningPDU);
                break;
            }
            case MeshConstants.GPCF_TRANSACTION_ACK: {
                // Transaction Acknowledgment PDU
                parseTransactionAcknowledgmentPDU(devTransNum, provisioningPDU);
                break;
            }
            case MeshConstants.GPCF_TRANSACTION_CONTINUATION: {
                // Transaction Continuation PDU
                parseTransactionContinuationPDU(devTransNum, provisioningPDU);
                break;
            }
            case MeshConstants.GPCF_PROVISIONING_BEARER_CONTROL: {
                // Provisioning Bearer Control
                parseProvisioningBearerControl(provisioningPDU);
                break;
            }
        }
    }

    private void parseTransactionStartPDU(byte transNum, byte[] provisioningPDU) {
        int segN = (provisioningPDU[0] & 0xff) >> 2;
        int totleLen = ((provisioningPDU[1] & 0xff) << 8) | (provisioningPDU[2] & 0xff);
        int fcs = provisioningPDU[3] & 0xff;

        byte[] segment = DataUtil.subBytes(provisioningPDU, 4);

        synchronized (mTransactionLock) {
            TransactionPDU transactionPDU = mTransactionPDUMap.get(transNum);
            if (transactionPDU == null) {
                transactionPDU = new TransactionPDU();
                mTransactionPDUMap.put(transNum, transactionPDU);
            } else {
                if (transactionPDU.hasReceivedAll()) {
                    return;
                }
            }

            transactionPDU.transNum = transNum;
            transactionPDU.segN = segN;
            transactionPDU.totleLen = totleLen;
            transactionPDU.fcs = fcs;
            transactionPDU.addSegment(0, segment);

            if (segN > 0) {
                if (transactionPDU.hasReceivedAll()) {
                    stopAdv();
                    parseTransactionPDU(transactionPDU);
                }
            } else {
                stopAdv();
                parseTransactionPDU(transactionPDU);
            }
        }
    }

    private void parseTransactionContinuationPDU(byte transNum, byte[] provisioningPDU) {
        synchronized (mTransactionLock) {
            TransactionPDU transactionPDU = mTransactionPDUMap.get(transNum);
            if (transactionPDU == null) {
                transactionPDU = new TransactionPDU();
                mTransactionPDUMap.put(transNum, transactionPDU);
            } else {
                if (transactionPDU.hasReceivedAll()) {
                    return;
                }
            }

            int segmentIndex = (provisioningPDU[0] & 0xff) >> 2;
            byte[] segment = DataUtil.subBytes(provisioningPDU, 1);
            transactionPDU.addSegment(segmentIndex, segment);
            if (transactionPDU.hasReceivedAll()) {
                stopAdv();
                parseTransactionPDU(transactionPDU);
            }
        }
    }

    private void parseTransactionPDU(TransactionPDU transactionPDU) {
        byte[] data = transactionPDU.getData();
        boolean valid = transactionPDU.checkData(data);
        if (!valid) {
            mLog.w("parseTransactionPDU invalid data");
        }

        int provisiongType = data[0] & 0xff;
        switch (provisiongType) {
            case MeshConstants.PROVISIONING_TYPE_CAPABILITIES:
                parseCapabilities(transactionPDU.transNum, data);
                break;
            case MeshConstants.PROVISIONING_TYPE_PUBLIC_KEY:
                parseDevicePublicKey(transactionPDU.transNum, data);
                break;
            case MeshConstants.PROVISIONING_TYPE_CONFIRMATION:
                parseDeviceConfirmation(transactionPDU.transNum, data);
                break;
            case MeshConstants.PROVISIONING_TYPE_RANDOM:
                parseDeviceRandom(transactionPDU.transNum, data);
                break;
            case MeshConstants.PROVISIONING_TYPE_COMPLETE:
                parseDeviceComplete(transactionPDU.transNum, data);
                break;
            case MeshConstants.PROVISIONING_TYPE_FAILED:
                parseDeviceFailed(transactionPDU.transNum, data);
                break;
        }
    }

    private void parseTransactionAcknowledgmentPDU(byte transNum, byte[] provisioningPDU) {
        if (transNum == mInviteAckTN) {
            stopAdv();
        } else if (transNum == mStartAckTN) {
            stopAdv();
            postProvisionerPublicKey();
        } else if (transNum == mAppPublicKeyAckTN) {
            stopAdv();
        } else if (transNum == mProvisonorConfirmationTN) {
            stopAdv();
        } else if (transNum == mProvisionorRandomTN) {
            stopAdv();
        } else if (transNum == mProvisionorDataTN) {
            stopAdv();
        }
    }

    private void parseProvisioningBearerControl(byte[] provisioningPDU) {
        int header = provisioningPDU[0] & 0xff;

        int opcode = (header >> 2) & 0xff;
        if (opcode == 0x01) {
            // TODO Ack
            stopAdv();

            invite();
        } else if (opcode == 0x02) {
            // TODO close link
            stopAdv();
        }
    }

    public void provisioning(String deviceName, Network network) {
        mNodeName = deviceName;
        mNetwork = network;

        initEcc();

        MeshObjectBox.getInstance().deleteNode(mDevice.getAddress());;

        openLink();
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

    private void openLink() {
        mLinkID = new byte[4];
        new Random().nextBytes(mLinkID);
        byte transactionNum = 0;

        byte opcode = 0x00;
        byte[] header = new byte[]{(byte) ((opcode << 2) | 3)};
        byte[] provisioningPDU = DataUtil.mergeBytes(header, mDeviceUUID);

        byte[] data = DataUtil.mergeBytes(mLinkID, new byte[]{transactionNum}, provisioningPDU);
        startAdv(data);
    }

    private void invite() {
        int type = MeshConstants.PROVISIONING_TYPE_INVITE;
        byte[] field = new byte[]{0x00};
        mInviteData = field;
        byte transNum = appTransNumGetAndIncreament();
        mInviteAckTN = transNum;

        advProvisioningPDU(transNum, type, field);
    }

    private void parseCapabilities(byte transNum,byte[] data) {
        mCapabilitiesData = DataUtil.subBytes(data, 1);
        mElementNum = mCapabilitiesData[0] & 0xff;
        long[] addrArray = MeshUtils.generateContinuousUnicastAddress(mElementNum);
        if (addrArray == null) {
            mUnicastAddr = MeshUtils.generateUnicastAddress();
        } else {
            mUnicastAddr = addrArray[0];
        }

        advAck(transNum);

        mExecutorService.execute(() -> {
            try {
                Thread.sleep(POST_ACK_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            } finally {
                stopAdv();
            }

            AdvProvisioner.this.start();
        });
    }

    private void start() {
        int type = MeshConstants.PROVISIONING_TYPE_START;
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
        byte transNum = appTransNumGetAndIncreament();
        mStartAckTN = transNum;

        advProvisioningPDU(transNum, type, field);
    }

    private void postProvisionerPublicKey() {
        int type = MeshConstants.PROVISIONING_TYPE_PUBLIC_KEY;
        byte[] publicXBytes = DataUtil.hexStringToBigEndianBytes(mECPublicKeyX.toString(16));
        byte[] publicYBytes = DataUtil.hexStringToBigEndianBytes(mECPublicKeyY.toString(16));
        byte[] publicKeyBytes = DataUtil.mergeBytes(publicXBytes, publicYBytes);
        mAppPublicKeyData = publicKeyBytes;
        byte transNum = appTransNumGetAndIncreament();
        mAppPublicKeyAckTN = transNum;

        advProvisioningPDU(transNum, type, publicKeyBytes);
    }

    private void parseDevicePublicKey(byte transNum, byte[] data) {
        int length = 32;
        byte[] deviceXData = new byte[length];
        byte[] deviceYData = new byte[length];
        System.arraycopy(data, 1, deviceXData, 0, length);
        System.arraycopy(data, 33, deviceYData, 0, length);
        BigInteger deviceX = new BigInteger(DataUtil.bigEndianBytesToHexString(deviceXData), 16);
        BigInteger deviceY = new BigInteger(DataUtil.bigEndianBytesToHexString(deviceYData), 16);

        BigInteger ecdhSecret = MeshAlgorithmUtils.generateECDHSecret(mECPrivateKey, deviceX, deviceY);
        assert ecdhSecret != null;
        mDHSecret = DataUtil.hexStringToBigEndianBytes(ecdhSecret.toString(16));
        mDevPublicKeyData = DataUtil.mergeBytes(deviceXData, deviceYData);

        advAck(transNum);

        mExecutorService.execute(() -> {
            try {
                Thread.sleep(POST_ACK_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            } finally {
                stopAdv();
            }

            postProvisonorConfirmation();
        });
    }

    private byte[] getConfirmationData(byte[] random, byte[] authValue) {
        byte[] key = MeshAlgorithmUtils.k1(mDHSecret, mSaltData, MeshConstants.BYTES_PRCK);
        byte[] random_authValue = DataUtil.mergeBytes(random, authValue);
        return MeshAlgorithmUtils.AES_CMAC(key, random_authValue);
    }

    private void postProvisonorConfirmation() {
        int type = MeshConstants.PROVISIONING_TYPE_CONFIRMATION;

        byte[] input = DataUtil.mergeBytes(mInviteData, mCapabilitiesData, mStartData, mAppPublicKeyData,
                mDevPublicKeyData);
        mSaltData = MeshAlgorithmUtils.s1(input);
        mAppRandomData = MeshUtils.generateRandom();
        byte[] confirmation = getConfirmationData(mAppRandomData, MeshConstants.AUTH_VALUE_ZERO);

        byte transNum = appTransNumGetAndIncreament();
        mProvisonorConfirmationTN = transNum;

        advProvisioningPDU(transNum, type, confirmation);
    }

    private void parseDeviceConfirmation(byte transNum, byte[] data) {
        mLog.d("parseDeviceConfirmation");
        mDevConfirmationData = DataUtil.subBytes(data, 1);

        mExecutorService.execute(() -> {
            try {
                Thread.sleep(POST_ACK_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            } finally {
                stopAdv();
            }

            postProvisionorRandom();
        });

    }

    private void postProvisionorRandom() {
        mLog.d("postProvisionorRandom");
        int type = MeshConstants.PROVISIONING_TYPE_RANDOM;
        byte transNum = appTransNumGetAndIncreament();
        mProvisionorRandomTN = transNum;

        advProvisioningPDU(transNum, type, mAppRandomData);
    }

    private void parseDeviceRandom(byte transNum, byte[] data) {
        mLog.d("parseDeviceRandom");
        byte[] random = DataUtil.subBytes(data, 1);
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

            advAck(transNum);

            mExecutorService.execute(() -> {
                try {
                    Thread.sleep(POST_ACK_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    stopAdv();
                }

                postData();
            });
        } else {
            if (mMeshCallback != null) {
                mMeshCallback.onProvisionResult(this, -300, null);
            }
            stopAdv();
        }
    }

    private void postData() {
        int type = MeshConstants.PROVISIONING_TYPE_DATA;
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

        byte transNum = appTransNumGetAndIncreament();
        mProvisionorDataTN = transNum;

        advProvisioningPDU(transNum, type, encrypt);
    }

    private void parseDeviceComplete(byte transNum, byte[] data) {
        Log.d(TAG, "parseDeviceComplete");
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        byte[] devKey = getDeviceKey();
        String devKeyStr = DataUtil.bigEndianBytesToHexString(devKey);
        String uuidStr = DataUtil.bigEndianBytesToHexString(mDeviceUUID);
        // Save node db
        dbManager.saveNode(mDevice.getAddress(), uuidStr, devKeyStr, mNodeName, mUnicastAddr, mElementNum,
                mNetwork.getKeyIndex());

        if (mMeshCallback != null) {
            Node node = new Node();
            node.setMac(mDevice.getAddress());
            node.setUUID(uuidStr);
            node.setName(mNodeName);
            node.setDeviceKey(devKey);
            node.setUnicastAddress(mUnicastAddr);
            node.setElementCount(mElementNum);
            mMeshCallback.onProvisionResult(this, 0, node);
        }

        advAck(transNum);
        closeLink((byte) 0x00);
    }

    private void parseDeviceFailed(byte transNum, byte[] data) {
        mLog.w("parseDeviceFailed");
        if (mMeshCallback != null) {
            mMeshCallback.onProvisionResult(this, data[1] & 0xff, null);
        }

        advAck(transNum);
        closeLink((byte) 0x02);
    }

    private void closeLink(byte reason) {
        byte transactionNum = 0;
        byte opcode = 0x02;
        byte header = (byte) ((opcode << 2) | 3);

        byte[] data = DataUtil.mergeBytes(mLinkID, new byte[]{transactionNum, header, reason});
        startAdv(data);

        mExecutorService.execute(() -> {
            try {
                Thread.sleep(3500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } finally {
                stopAdv();
            }
        });
    }

    private byte[] getDeviceKey() {
        byte[] provisioningSalt = MeshAlgorithmUtils.s1(DataUtil.mergeBytes(mSaltData, mAppRandomData, mDevRandomData));
        return MeshAlgorithmUtils.k1(mDHSecret, provisioningSalt, MeshConstants.BYTES_PRDK);
    }

    private class LeScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            onScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        private void onScanResult(ScanResult result) {
            byte[] scanData = Objects.requireNonNull(result.getScanRecord()).getBytes();
            int length = scanData[0];
            int type = scanData[1];
            byte[] data = DataUtil.subBytes(scanData, 2, length);

            byte[] linkID = DataUtil.subBytes(data, 0, 4);
            if (!checkLinkID(linkID)) {
                return;
            }

            byte devTransNum = data[4];

            byte[] provisioningPDU = DataUtil.subBytes(data, 5);

            parse(type, devTransNum, provisioningPDU);
        }
    }

    private class LeAdvCallback extends AdvertiseCallback {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

            mLog.d("Adv onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            mLog.d("Adv onStartFailure " + errorCode);
        }
    }

    private class TransactionPDU {
        byte transNum;
        int segN = -1;
        int totleLen = -1;
        int fcs;

        final Map<Integer, byte[]> segmentMap = new HashMap<>();

        void addSegment(int index, byte[] data) {
            synchronized (segmentMap) {
                segmentMap.put(index, data);
            }
        }

        boolean hasReceivedAll() {
            synchronized (segmentMap) {
                if (segN < 0 || totleLen < 0) {
                    return false;
                }

                return segmentMap.size() == (segN + 1);
            }
        }

        byte[] getData() {
            byte[] result = {};
            for (int i = 0; i <= segN; i++) {
                byte[] data = segmentMap.get(i);
                result = DataUtil.mergeBytes(result, data);
            }

            return result;
        }

        boolean checkData(byte[] data) {
            if (data.length != totleLen) {
                mLog.w("ProvisioningData check data length invalid");
                return false;
            }

            int crc = calcCRC8(data);
            if (crc != fcs) {
                mLog.w("ProvisioningData check fcs value invalid");
            }

            return crc == fcs;
        }
    }
}
