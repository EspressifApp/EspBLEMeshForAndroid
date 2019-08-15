package com.espressif.blemesh.client;

import android.annotation.SuppressLint;

import com.espressif.blemesh.client.abs.MeshCommunicationClient;
import com.espressif.blemesh.client.callback.MessageCallback;
import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.model.message.Message;
import com.espressif.blemesh.model.message.custom.FastGroupBindMessage;
import com.espressif.blemesh.model.message.custom.FastGroupUnbindMessage;
import com.espressif.blemesh.model.message.custom.OtaNBVNMessage;
import com.espressif.blemesh.model.message.custom.OtaStartMessage;
import com.espressif.blemesh.model.message.standard.LightHSLGetMessage;
import com.espressif.blemesh.model.message.standard.LightHSLSetMessage;
import com.espressif.blemesh.model.message.standard.proxyconfiguration.AddAddressesToFilterMessage;
import com.espressif.blemesh.model.message.standard.AppKeyAddMessage;
import com.espressif.blemesh.model.message.standard.CompositionDataGetMessage;
import com.espressif.blemesh.model.message.custom.FastProvInfoSetMessage;
import com.espressif.blemesh.model.message.custom.FastProvNodeAddrGetMessage;
import com.espressif.blemesh.model.message.standard.GenericOnOffMessage;
import com.espressif.blemesh.model.message.standard.ModelAppBindMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionAddMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionDeleteMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionDeleteAllMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionOverwriteMessage;
import com.espressif.blemesh.model.message.standard.RelaySetMessage;
import com.espressif.blemesh.model.message.standard.proxyconfiguration.SetFilterTypeMessage;
import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Element;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.utils.MeshAlgorithmUtils;
import com.espressif.blemesh.utils.MeshUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

public class MeshGattMessager extends MeshCommunicationClient implements IMeshMessager {
    private static final int NONCE_TYPE_APP = 1;
    private static final int NONCE_TYPE_DEVICE = 2;
    private static final int NONCE_TYPE_NETWORK = 3;
    private static final int NONCE_TYPE_PROXY = 4;

    private static final int ACCESS_SEGMENT_LENGTH = 16;
    private static final int CONTROL_SEGMENT_LENGTH = 12;
    private static final int SEGMENT_LENGTH_MAX = 12;

    private EspLog mLog = new EspLog(getClass());

    private volatile boolean mClosed = false;

    private MessageCallback mMessageCallback;

    private final Object mWriteLock = new Object();
    private LinkedBlockingQueue<byte[]> mPostDataQueue;
    private WriteThread mWriteThread;

    private LinkedList<byte[]> mRecvNotifyList;

    private Map<Integer, App> mAppMap;
    private Map<Long, Node> mNodeMap;
    private Network mNetwork;
    private byte[] mAppAddr;

    private Map<String, SegmentedMessage> mSegmentedMessageMap = new HashMap<>();

    private volatile SubscriptionOp mLastSubscriptionOp;

    private enum SubscriptionOp {
        Add, Delete, Overwrite, DeleteAll;
    }

    @SuppressLint("UseSparseArrays")
    MeshGattMessager() {
        mAppMap = new HashMap<>();
        mNodeMap = new HashMap<>();
        mPostDataQueue = new LinkedBlockingQueue<>();
        mRecvNotifyList = new LinkedList<>();
        mWriteThread = new WriteThread();
        mWriteThread.start();
    }

    @Override
    public void release() {
        super.release();

        mClosed = true;
        mAppMap.clear();
        mNodeMap.clear();
        mWriteThread.interrupt();
    }

    void setAppAddr(byte[] appAddr) {
        mAppAddr = appAddr;
    }

    @Override
    public void setNetwork(Network network) {
        mNetwork = network;
    }

    @Override
    public Network getNetwork() {
        return mNetwork;
    }

    @Override
    public void setMessageCallback(MessageCallback messageCallback) {
        mMessageCallback = messageCallback;
    }

    @Override
    public void onNotification(byte[] data) {
        parseNotification(data);
    }

    @Override
    public void onWrote(byte[] data) {
        synchronized (mWriteLock) {
            mWriteLock.notify();
        }
    }

    private byte[] getEncKey(int type, int akf, int aid, byte[] deviceAddr) {
        switch (type) {
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION: {
                return mNetwork.getEncryptionKey();
            }
            default: {
                if (akf == 1) {
                    return mAppMap.get(aid).getAppKey();
                } else {
                    Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
                    return node == null ? null : node.getDeviceKey();
                }
            }
        }
    }

    private byte[] getDecNonce(int type, int akf, int szmic, byte[] seq, byte[] src, byte[] dst) {
        switch (type) {
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION: {
                return getProxyNonce(seq, src, mNetwork.getIVIndexBytes());
            }
            default: {
                if (akf == 1) {
                    return getAppNonce(szmic, seq, src, dst, mNetwork.getIVIndexBytes());
                } else {
                    return getDevNonce(szmic, seq, src, dst, mNetwork.getIVIndexBytes());
                }
            }
        }
    }

    private byte[] getNonce(byte type, byte pad, byte[] seq, byte[] srcAddr, byte[] dstAddr, byte[] ivIndex) {
        byte[] nonce = new byte[13];
        nonce[0] = type;
        nonce[1] = pad;
        nonce[2] = seq[0];
        nonce[3] = seq[1];
        nonce[4] = seq[2];
        nonce[5] = srcAddr[0];
        nonce[6] = srcAddr[1];
        nonce[7] = dstAddr[0];
        nonce[8] = dstAddr[1];
        nonce[9] = ivIndex[0];
        nonce[10] = ivIndex[1];
        nonce[11] = ivIndex[2];
        nonce[12] = ivIndex[3];

        return nonce;
    }

    private byte[] getNetNonce(int ctl, int ttl, byte[] seq, byte[] srcAddr, byte[] ivIndex) {
        byte pad = getCtlTtl(ctl, ttl);
        byte[] dst = {0x00, 0x00};
        return getNonce((byte) 0x00, pad, seq, srcAddr, dst, ivIndex);
    }

    private byte[] getAppNonce(int szmic, byte[] seq, byte[] srcAddr, byte[] dstAddr, byte[] ivIndex) {
        byte pad = (byte) ((szmic << 7) & 0xff);
        return getNonce((byte) 0x01, pad, seq, srcAddr, dstAddr, ivIndex);
    }

    private byte[] getDevNonce(int szmic, byte[] seq, byte[] srcAddr, byte[] dstAddr, byte[] ivIndex) {
        byte pad = (byte) ((szmic << 7) & 0xff);
        return getNonce((byte) 0x02, pad, seq, srcAddr, dstAddr, ivIndex);
    }

    private byte[] getProxyNonce(byte[] seq, byte[] srcAddr, byte[] ivIndex) {
        byte pad = 0x00;
        byte[] padBytes = {0x00, 0x00};
        return getNonce((byte) 0x03, pad, seq, srcAddr, padBytes, ivIndex);
    }

    private byte getCtlTtl(int ctl, int ttl) {
        return (byte) ((ctl << 7 & 0xff) | (ttl & 0x7f));
    }

    private int getCtl(int ctl_ttl) {
        return ctl_ttl >> 7 & 1;
    }

    private int getTtl(int ctl_ttl) {
        return ctl_ttl & 0x7f;
    }

    private String getAidBitString(int aid) {
        StringBuilder aidSB = new StringBuilder(Integer.toBinaryString(aid));
        while (aidSB.length() < 6) {
            aidSB.insert(0, "0");
        }
        while (aidSB.length() > 6) {
            aidSB.deleteCharAt(0);
        }
        return aidSB.toString();
    }

    private byte[] getUnsegmentedHeader(int aid) {
        long seg = 0;
        long akf = aid == 0 ? 0 : 1;
        String aidBitString = getAidBitString(aid);
        int i = Integer.parseInt(seg + akf + aidBitString, 2);
        return new byte[]{(byte) i};
    }

    private byte[] getSegmentedHeader(int szmic, long seq, long segO, long segN, int aid) {
        long seg = 1;
        long akf = aid == 0 ? 0 : 1;
        String aidBitString = getAidBitString(aid);

        StringBuilder sb = new StringBuilder();
        sb.append(seg);
        sb.append(akf);
        sb.append(aidBitString);
        sb.append(szmic);
        StringBuilder seqZeroStr = new StringBuilder(Long.toBinaryString(MeshUtils.getSeqZero(seq)));
        while (seqZeroStr.length() < 13) {
            seqZeroStr.insert(0, "0");
        }
        while (seqZeroStr.length() > 13) {
            seqZeroStr.deleteCharAt(0);
        }
        sb.append(seqZeroStr);

        StringBuilder segOStr = new StringBuilder(Long.toBinaryString(segO));
        while (segOStr.length() < 5) {
            segOStr.insert(0, "0");
        }
        while (segOStr.length() > 5) {
            segOStr.deleteCharAt(0);
        }
        sb.append(segOStr);

        StringBuilder segNStr = new StringBuilder(Long.toBinaryString(segN));
        while (segNStr.length() < 5) {
            segNStr.insert(0, "0");
        }
        while (segNStr.length() > 5) {
            segNStr.deleteCharAt(0);
        }
        sb.append(segNStr);

        long al = Long.parseLong(sb.toString(), 2);
        byte[] head = new byte[4];
        for (int i = 0; i < head.length; i++) {
            head[i] = (byte) (al >> (8 * (head.length - 1 -i)));
        }

        return head;
    }

    private void parseNotification(byte[] data) {
        mLog.d("parseNotification " + DataUtil.bigEndianBytesToHexString(data));
        int proxy = data[0] & 0xff;
        int proxySar = proxy >> 6;
        int proxyType = proxy & 63;
        mLog.d(String.format(Locale.ENGLISH, "sar=%d, type=%d", proxySar, proxyType));

        switch (proxyType) {
            case MeshConstants.PROXY_TYPE_NETWORK_PDU:
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION:
                break;
            default:
                mLog.w("parseNotification unsupport proxy type");
                return;
        }

        switch (proxySar) {
            case MeshConstants.PROXY_SAR_COMPLETE:
                break;
            case MeshConstants.PROXY_SAR_FIRST:
                mRecvNotifyList.clear();
                mRecvNotifyList.add(data);
                return;
            case MeshConstants.PROXY_SAR_CONTINUATION:
                mRecvNotifyList.add(DataUtil.subBytes(data, 1));
                return;
            case MeshConstants.PROXY_SAR_LAST:
                mRecvNotifyList.add(DataUtil.subBytes(data, 1));

                data = new byte[0];
                for (byte[] bytes : mRecvNotifyList) {
                    data = DataUtil.mergeBytes(data, bytes);
                }
                mRecvNotifyList.clear();
                break;
            default:
                return;
        }

        byte nid = data[1];
        if (nid != mNetwork.getNid()) {
            // TODO
            mLog.e("Nid error");
            return;
        }
        byte[] obData = {data[2], data[3], data[4], data[5], data[6], data[7]};

        byte[] encTransport = new byte[data.length - 8];
        System.arraycopy(data, 8, encTransport, 0, encTransport.length);

        byte[] privacyRandom = new byte[7];
        System.arraycopy(encTransport, 0, privacyRandom, 0, 7);
        byte[] pecb = MeshAlgorithmUtils.e(mNetwork.getPrivacyKey(),
                DataUtil.mergeBytes(new byte[5], mNetwork.getIVIndexBytes(), privacyRandom));
        byte[] pecb05 = DataUtil.subBytes(pecb, 0, 6);
        byte[] ctlTtlSeqSrc = new BigInteger(obData)
                .xor(new BigInteger(pecb05))
                .toByteArray();
        if (ctlTtlSeqSrc.length < 6) {
            int buLen = 6 - ctlTtlSeqSrc.length;
            byte[] buData = new byte[buLen];
            ctlTtlSeqSrc = DataUtil.mergeBytes(buData, ctlTtlSeqSrc);
        }
        int ctl_ttl = ctlTtlSeqSrc[0] & 0xff;
        int ctl = getCtl(ctl_ttl);
        int ttl = getTtl(ctl_ttl);
        byte[] seq = {ctlTtlSeqSrc[1], ctlTtlSeqSrc[2], ctlTtlSeqSrc[3]};
        byte[] src = {ctlTtlSeqSrc[4], ctlTtlSeqSrc[5]};
        int netMicLen = ctl == 0 ? 32 : 64;

        byte[] nonce = null;
        switch (proxyType) {
            case MeshConstants.PROXY_TYPE_NETWORK_PDU: {
                nonce = getNetNonce(ctl, ttl, seq,  src, mNetwork.getIVIndexBytes());
                break;
            }
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION: {
                nonce = getProxyNonce(seq, src, mNetwork.getIVIndexBytes());
                break;
            }
        }
        byte[] decTransPDU = MeshAlgorithmUtils.AES_CCM_Decrypt(mNetwork.getEncryptionKey(), nonce, netMicLen,
                encTransport);
        byte[] dst = {decTransPDU[0], decTransPDU[1]};

        byte[] transportPDU = DataUtil.subBytes(decTransPDU, 2);
        if (ctl == 0) {
            // Access Message
            byte[] lowerTransPDU = transportPDU;
            int ltPDU0 = lowerTransPDU[0] & 0xff;
            int seg = ltPDU0 >> 7;

            if (seg == 0) {
                // Unseg Access Message
                int afk = (ltPDU0 >> 6) & 1;
                int aid = ltPDU0 & 63;
                final int micBitSize = 32;

                byte[] upperTransPDU = DataUtil.subBytes(lowerTransPDU, 1);

                byte[] decKey = getEncKey(proxyType, afk, aid, src);
                byte[] decNonce = getDecNonce(proxyType, afk, 0, seq, src, dst);
                byte[] accessPayload = MeshAlgorithmUtils.AES_CCM_Decrypt(decKey, decNonce, micBitSize, upperTransPDU);

                parseNetworkPDUAccessMessage(src, accessPayload);
            } else {
                // Seg Access Message
                int afk = (ltPDU0 >> 6) & 1;
                int aid = ltPDU0 & 63;

                StringBuilder headerBitSB = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    int integer = lowerTransPDU[i] & 0xff;
                    StringBuilder string = new StringBuilder(Integer.toBinaryString(integer));
                    while (string.length() < 8) {
                        string.insert(0, "0");
                    }
                    headerBitSB.append(string);
                }

                char szmicChar = headerBitSB.charAt(8);
                int szmic = Integer.parseInt(String.valueOf(szmicChar), 2);
                int transMicLen = szmic == 0 ? 32 : 64;

                String seqZeroStr = headerBitSB.substring(9, 22);
//            int seqZero = Integer.parseInt(seqZeroStr, 2);

                String segOStr = headerBitSB.substring(22, 27);
                int segO = Integer.parseInt(segOStr, 2);
                String segNStr = headerBitSB.substring(27);
                int segN = Integer.parseInt(segNStr, 2);

                byte[] segment = DataUtil.subBytes(lowerTransPDU, 4);

                SegmentedMessage segMsg = mSegmentedMessageMap.get(seqZeroStr);
                if (segMsg == null) {
                    segMsg = new SegmentedMessage();
                    mSegmentedMessageMap.put(seqZeroStr, segMsg);
                }
                if (segO == 0) {
                    segMsg.firstSeq = seq;
                }
                segMsg.appendSegment(segment);

                if (segO == segN) {
                    byte[] upperTransPDU = segMsg.getUpperTransportPDU();

                    byte[] decKey = getEncKey(proxyType, afk, aid, src);
                    byte[] decNonce = getDecNonce(proxyType, afk, szmic, segMsg.firstSeq, src, dst);
                    byte[] accessPayload = MeshAlgorithmUtils.AES_CCM_Decrypt(decKey, decNonce, transMicLen, upperTransPDU);
                    mSegmentedMessageMap.remove(seqZeroStr);

                    parseNetworkPDUAccessMessage(src, accessPayload);
                }
            }
        } else {
            // Control Message
            int pdu0 = transportPDU[0] & 0xff;
            switch (proxyType) {
                case MeshConstants.PROXY_TYPE_NETWORK_PDU:
                    int seg = pdu0 >> 7;
                    if (seg == 0) {
                        int opCode = pdu0 & 0x7f;
                        byte[] payload = Arrays.copyOf(transportPDU, transportPDU.length);
                        payload[0] = (byte) opCode;

                        parseNetworkPDUControlMessage(src, payload);
                    } else {
                        // TODO Seg Control Message
                        mLog.d("Receive Seg Network Control Message");
                    }

                    break;
                case MeshConstants.PROXY_TYPE_PROXY_CONGURATION:
                    parseProxyConguration(src, transportPDU);
                    break;
            }
        }
    }

    private void parseNetworkPDUControlMessage(byte[] deviceAddr, byte[] payload) {
        mLog.i("parseNetworkPDUControlMessage " + DataUtil.bigEndianBytesToHexString(payload));
        switch (payload[0] & 0xff) {
            case 0x00: {
                // Segment Acknowledgment message
                mLog.d("Segment Acknowledgment message");
                break;
            }
        }
    }

    private void parseNetworkPDUAccessMessage(byte[] deviceAddr, byte[] payload) {
        mLog.i("parseNetworkPDUAccessMessage " + DataUtil.bigEndianBytesToHexString(payload));
        switch (payload[0] & 0xff) {
            case 0x02: {
                // Composition Data Status
                parseCompositionDataStatus(deviceAddr, payload);
                break;
            }
            case 0x80: {
                switch (payload[1] & 0xff) {
                    case 0x03: {
                        // App Key Status
                        parseAppKeyStatus(deviceAddr, payload);
                        break;
                    }
                    case 0x14: {
                        // Gatt Proxy Status
//                        parseGattProxyStatus(payload);
                        break;
                    }
                    case 0x1f: {
                        // Model Subscription Status
                        parseModelSubscriptionStatus(deviceAddr, payload);
                        break;
                    }
                    case 0x28: {
                        // Relay Status
                        parseRelayStatus(payload);
                        break;
                    }
                    case 0x3e: {
                        // Model App Status
                        parseModelAppStatus(deviceAddr, payload);
                        break;
                    }
                } // end switch opcode index 1

                break;
            }
            case 0x82: {
                switch (payload[1] & 0xff) {
                    case 0x04: {
                        // Generic OnOff Status
                        parseGenericOnOffStatus(payload);
                        break;
                    }
                    case 0x78: {
                        // Light HSL Status
                        parseLightHSLStatus(payload);
                        break;
                    }
                }
                break;
            }
            case 0xC1: {
                if ((payload[1] & 0xff) == 0xE5 && (payload[2] & 0xff) == 0x02) {
                    parseFastProvStatus(deviceAddr, payload);
                }
                break;
            }
            case 0xC7: {
                if ((payload[1] & 0xff) == 0xE5 && (payload[2] & 0xff) == 0x02) {
                    // Fast Prov Node Addr Status
                    parseFastProvNodeAddrStatus(deviceAddr, payload);
                }
                break;
            }
            case 0xCB: {
                // Need OTA Update Notification
                parseNeedOTAUpdateNotification(deviceAddr, payload);
                break;
            }
            case 0xCD: {
                // OTA Start Response
                parseOTAStartResponse(deviceAddr, payload);
            }
        }// end switch opcode index 0
    }

    private void parseProxyConguration(byte[] src, byte[] payload) {
        mLog.i("parseProxyConguration " + DataUtil.bigEndianBytesToHexString(payload));
        switch (payload[0] & 0xff) {
            case 0x03: {
                // Filter Status
                int filterType = payload[1] & 0xff; // 0 is white, 1 is black
                int size = (payload[2] & 0xff << 8) | (payload[3] & 0xff);
                mLog.d("Filter type = " + filterType + ", size = " + size);
                break;
            }
        }
    }

    private byte[] getAccessPayload(byte[] opcode, byte[] parameters) {
        if (parameters == null) {
            return opcode;
        } else {
            return DataUtil.mergeBytes(opcode, parameters);
        }
    }

    private byte[] getUpperTransportPDU(byte[] accessPayload, int szmic, long seq, byte[] dst,
                                        byte[] key, int nonceType) {
        int transMicLen = szmic == 0 ? 32 : 64;
        byte[] seqBytes = MeshUtils.getSequenceBytes(seq);
        byte[] nonce = null;
        switch (nonceType) {
            case NONCE_TYPE_APP:
                nonce = getAppNonce(szmic, seqBytes, mAppAddr, dst, mNetwork.getIVIndexBytes());
                break;
            case NONCE_TYPE_DEVICE:
                nonce = getDevNonce(szmic, seqBytes, mAppAddr, dst, mNetwork.getIVIndexBytes());
                break;
            case NONCE_TYPE_PROXY:
                nonce = getProxyNonce(seqBytes, mAppAddr, mNetwork.getIVIndexBytes());
                break;
        }
        return MeshAlgorithmUtils.AES_CCM_Encrypt(key, nonce, transMicLen, accessPayload);
    }

    private byte[] getLowerTransportPDU(byte[] upperTransportPDU, boolean seg, int szmic,
                                        long seq, long segO, long segN, int aid) {
        byte[] header;
        if (seg) {
            header = getSegmentedHeader(szmic, seq, segO, segN, aid);
        } else {
            header = getUnsegmentedHeader(aid);
        }

        return DataUtil.mergeBytes(header, upperTransportPDU);
    }

    private byte[] getPDU(byte[] transportPDU, byte[] nonce, int ctl, int ttl, byte[] seq, byte[] dst) {
        int micBitSize = ctl == 0 ? 32 : 64;
        byte[] encTransport = MeshAlgorithmUtils.AES_CCM_Encrypt(mNetwork.getEncryptionKey(), nonce, micBitSize,
                DataUtil.mergeBytes(dst, transportPDU));

        byte[] privacyRandom = new byte[7];
        System.arraycopy(encTransport, 0, privacyRandom, 0, 7);
        byte[] pecb = MeshAlgorithmUtils.e(mNetwork.getPrivacyKey(),
                DataUtil.mergeBytes(new byte[5], mNetwork.getIVIndexBytes(), privacyRandom));
        byte[] pecb05 = new byte[6];
        System.arraycopy(pecb, 0, pecb05, 0, 6);
        byte[] obfuscatedData = new BigInteger(DataUtil.mergeBytes(new byte[]{getCtlTtl(ctl, ttl)}, seq, mAppAddr))
                .xor(new BigInteger(pecb05))
                .toByteArray();
        return DataUtil.mergeBytes(new byte[]{mNetwork.getNid()}, obfuscatedData, encTransport);
    }

    private byte[] getNetworkPDU(byte[] lowerTransportPDU, int ctl, int ttl, long seq, byte[] dst) {
        byte[] seqBytes = MeshUtils.getSequenceBytes(seq);
        byte[] netNonce = getNetNonce(ctl, ttl, seqBytes , mAppAddr, mNetwork.getIVIndexBytes());
        return getPDU(lowerTransportPDU, netNonce, ctl, ttl, seqBytes, dst);
    }

    private byte[] getProxyPDU(byte[] transportPDU, int ctl, int ttl, long seq, byte[] dst) {
        byte[] seqBytes = MeshUtils.getSequenceBytes(seq);
        byte[] proxyNonce = getProxyNonce(seqBytes, mAppAddr, mNetwork.getIVIndexBytes());
        return getPDU(transportPDU, proxyNonce, ctl, ttl, seqBytes, dst);
    }

    private synchronized void addPDUDataInQueue(int proxyType, byte[] pdu) {
        final int headLength = 1;
        if (pdu.length + headLength <= getAvailableGattMTU()) {
            byte[] data = MeshUtils.getData(MeshConstants.PROXY_SAR_COMPLETE, proxyType, pdu);
            mPostDataQueue.add(data);
        } else {
            int splitLengthLimit = getAvailableGattMTU() - headLength;
            List<byte[]> splits = DataUtil.splitBytes(pdu, splitLengthLimit);
            for (int i = 0; i < splits.size(); i++) {
                int sar;
                if (i == 0) {
                    sar = MeshConstants.PROXY_SAR_FIRST;
                } else if (i == splits.size() - 1) {
                    sar = MeshConstants.PROXY_SAR_LAST;
                } else {
                    sar = MeshConstants.PROXY_SAR_CONTINUATION;
                }

                byte[] data = MeshUtils.getData(sar, proxyType, splits.get(i));
                mPostDataQueue.add(data);
            }
        }
    }

    private void postNetworkPDU(byte[] opcode, byte[] parameters, int ctl, int ttl, long dstAddr,
                                Node node, App app, Message.SecurityKey securityKey, int proxyType) {
        if (app != null) {
            mAppMap.put(app.getAid(), app);
        }
        if (node != null) {
            mNodeMap.put(node.getUnicastAddress(), node);
        }

        long firstSeq = mNetwork.seqIncrementAndGet();
        boolean seg = false;
        int szmic = 0;

        byte[] accessPayload = getAccessPayload(opcode, parameters);
//        mLog.i("PostNetworkPayload = " + DataUtil.bigEndianBytesToHexString(accessPayload));

        byte[] dst = MeshUtils.addressLongToBigEndianBytes(dstAddr);

        int nonceType;
        byte[] key;
        int aid;
        if (securityKey == Message.SecurityKey.DeviceKey) {
            nonceType = NONCE_TYPE_DEVICE;
        } else {
            nonceType = NONCE_TYPE_APP;
        }

        switch (securityKey) {
            case DeviceKey:
                assert node != null;
                key = node.getDeviceKey();
                aid = 0;
                break;
            case AppKey:
                assert app != null;
                key = app.getAppKey();
                aid = app.getAid();
                break;
            default:
                mLog.w("Post NetworkPDU unknown securityKey " + securityKey);
                return;
        }
        byte[] upperTransportPDU = getUpperTransportPDU(accessPayload, szmic, firstSeq, dst, key, nonceType);

        final int segmentMaxLength = SEGMENT_LENGTH_MAX; // ctl == 0 ? ACCESS_SEGMENT_LENGTH : CONTROL_SEGMENT_LENGTH;
        int segmentCount = upperTransportPDU.length / segmentMaxLength;
        segmentCount += upperTransportPDU.length % segmentMaxLength == 0 ? 0 : 1;
        if (segmentCount > 1) {
            seg = true;
        }
        long segN = segmentCount - 1;
        for (long segO = 0; segO <= segN; segO++) {
            byte[] segment;
            if (!seg) {
                segment = upperTransportPDU;
            } else {
                int begin = (int) (segO * segmentMaxLength);
                if (segO < segN) {
                    segment = DataUtil.subBytes(upperTransportPDU, begin, segmentMaxLength);
                } else {
                    segment = DataUtil.subBytes(upperTransportPDU, begin);
                }
            }

            byte[] lowerTransportPDU = getLowerTransportPDU(segment, seg, szmic, firstSeq, segO, segN, aid);

            long seq = segO == 0 ? firstSeq : mNetwork.seqIncrementAndGet();
            byte[] networkPDU = getNetworkPDU(lowerTransportPDU, ctl, ttl, seq, dst);
            mLog.i("Post NetworkPDU = " + DataUtil.bigEndianBytesToHexString(networkPDU));

            addPDUDataInQueue(proxyType, networkPDU);
        }
    }

    private void postProxyPDU(byte[] opcode, byte[] parameters, int ctl, int ttl, long dstAddr,
                              Node node, App app, Message.SecurityKey securityKey, int proxyType) {
        if (app != null) {
            mAppMap.put(app.getAid(), app);
        }
        if (node != null) {
            mNodeMap.put(node.getUnicastAddress(), node);
        }

        byte[] transpostPDU = DataUtil.mergeBytes(opcode, parameters);
        long seq = mNetwork.seqIncrementAndGet();
        byte[] proxyPDU = getProxyPDU(transpostPDU, ctl, ttl, seq, MeshUtils.addressLongToBigEndianBytes(dstAddr));
        mLog.i("Post ProxyPDU = " + DataUtil.bigEndianBytesToHexString(proxyPDU));

        addPDUDataInQueue(proxyType, proxyPDU);
    }

    private void __postMessage(byte[] opcode, byte[] parameters, int ctl, int ttl, long dstAddr,
                               Node node, App app, Message.SecurityKey securityKey, int proxyType) {
        switch (proxyType) {
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION: {
                postProxyPDU(opcode, parameters, ctl, ttl, dstAddr, node, app, securityKey, proxyType);
                break;
            }
            case MeshConstants.PROXY_TYPE_NETWORK_PDU: {
                postNetworkPDU(opcode, parameters, ctl, ttl, dstAddr, node, app, securityKey, proxyType);
            }
        }
    }

    @Override
    public void postMessage(Message message) {
        mLog.d("postMessage " + message.getClass().getSimpleName());
        int postCount = message.getPostCount();
        for (int i = 0; i < postCount; i++) {
            __postMessage(message.getOpCode(), message.getParameters(), message.getCTL(), message.getTTL(),
                    message.getDstAddress(), message.getNode(), message.getApp(), message.getSecurityKey(),
                    message.getProxyType());
        }
    }

    public void setFilterType(SetFilterTypeMessage message) {
        postMessage(message);
    }

    public void addAddressesToFilter(AddAddressesToFilterMessage message) {
        postMessage(message);
    }

    @Override
    public void appKeyAdd(AppKeyAddMessage message) {
        postMessage(message);
    }

    private void parseAppKeyStatus(byte[] deviceAddr, byte[] payload) {
        Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
        if (node == null) {
            return;
        }

        int status = payload[2];
        byte[] indexes = {payload[3], payload[4], payload[5]};
        long[] longIndexes = MeshUtils.getIndexesWithThreeBytes(indexes);
        long netKeyIndex = longIndexes[0];
        long appKeyIndex = longIndexes[1];
        mLog.i("Status = " + status + " netIndex = " + netKeyIndex +
                " appIndex = " + appKeyIndex);

        if (status == 0) {
            node.addAppKeyIndex(appKeyIndex);
            MeshObjectBox dbManager = MeshObjectBox.getInstance();
            dbManager.saveAppNode(appKeyIndex, node.getMac());
        }

        if (mMessageCallback != null) {
            mMessageCallback.onAppKeyStatus(status, netKeyIndex, appKeyIndex);
        }
    }

    @Override
    public void compositionDataGet(CompositionDataGetMessage message) {
        postMessage(message);
    }

    private void parseCompositionDataStatus(byte[] deviceAddr, byte[] payload) {
        Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
        if (node == null) {
            return;
        }

        boolean saveDB = !node.hasCompositionData();
        int page = payload[1] & 0xff;
        byte[] data = DataUtil.subBytes(payload, 2);

        byte[] cid = {data[1], data[0]};
        byte[] pid = {data[3], data[2]};
        byte[] vid = {data[5], data[4]};
        byte[] crpl = {data[7], data[6]};
        byte[] features = {data[9], data[8]};

        node.setCid(MeshUtils.bigEndianBytesToLong(cid));
        node.setPid(MeshUtils.bigEndianBytesToLong(pid));
        node.setVid(MeshUtils.bigEndianBytesToLong(vid));
        node.setCrpl(MeshUtils.bigEndianBytesToLong(crpl));
        node.setFeatures(MeshUtils.bigEndianBytesToLong(features));

        byte[] elementsData = DataUtil.subBytes(data, 10);
        int offset = 0;
        for (long index = 0L; ; index++) {
            long elementAddress = node.getUnicastAddress() + index;
            Element element = node.getElementForAddress(elementAddress);
            if (element == null) {
                element = new Element();
                element.setUnicastAddress(elementAddress);
                node.addElement(element);
            }
            byte[] loc = {elementsData[offset + 1], elementsData[offset]};
            element.setLoc(MeshUtils.bigEndianBytesToLong(loc));

            int numS = elementsData[offset + 2] & 0xff;
            int numV = elementsData[offset + 3] & 0xff;

            offset += 4;

            for (int i = 0; i < numS; i++) {
                byte[] sigID = {elementsData[offset + 1], elementsData[offset]};
                String modelId = DataUtil.bigEndianBytesToHexString(sigID);
                Model model = element.getModeForId(modelId);
                if (model == null) {
                    model = new Model();
                    model.setId(modelId);
                    element.addModel(model);
                }
                model.setElementAddress(elementAddress);
                model.setNodeMac(node.getMac());

                offset += sigID.length;
            }
            for (int i = 0; i < numV; i++) {
                byte[] vndID = {elementsData[offset + 3], elementsData[offset + 2],
                        elementsData[offset + 1], elementsData[offset]};
                String modelId = DataUtil.bigEndianBytesToHexString(vndID);
                Model model = element.getModeForId(modelId);
                if (model == null) {
                    model = new Model();
                    model.setId(modelId);
                    element.addModel(model);
                }
                model.setElementAddress(elementAddress);
                model.setNodeMac(node.getMac());

                offset += vndID.length;
            }

            if (offset >= elementsData.length) {
                break;
            }
        }

        if (saveDB) {
            MeshObjectBox dbManager = MeshObjectBox.getInstance();
            dbManager.updateNode(node.getMac(), node.getCid(), node.getPid(), node.getVid(),
                    node.getCrpl(), node.getFeatures());
            for (Element element : node.getElementList()) {
                dbManager.saveElement(element.getUnicastAddress(), node.getMac(), element.getLoc());

                for (Model model : element.getModelList()) {
                    dbManager.saveModel(model.getId(), model.getElementAddress(), model.getNodeMac());
                }
            }
        }

        if (mMessageCallback != null) {
            mMessageCallback.onCompositionDataStatus(MessageCallback.CODE_SUCCESS, page);
        }
    }

    @Override
    public void modelAppBind(ModelAppBindMessage message) {
        postMessage(message);
    }

    private void parseModelAppStatus(byte[] deviceAddr, byte[] payload) {
        Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
        if (node == null) {
            return;
        }

        int status = payload[2] & 0xff;

        long elementAddress = MeshUtils.bigEndianBytesToLong(new byte[]{payload[4], payload[3]});
        byte[] appKeyIndexBytes = {payload[6], payload[5]};
        long appKeyIndex = MeshUtils.getIndexWithTwoBytes(appKeyIndexBytes);
        byte[] modelIdBytesRV = DataUtil.subBytes(payload, 7);
        String modelId = DataUtil.littleEndianBytesToHexString(modelIdBytesRV);
        if (status == 0) {
            node.getElementForAddress(elementAddress)
                    .getModeForId(modelId)
                    .setAppKeyIndex(appKeyIndex);
            MeshObjectBox.getInstance().updaeModelAppKeyIndex(modelId, elementAddress, appKeyIndex);
        }
        if (mMessageCallback != null) {
            mMessageCallback.onModelAppStatus(status, appKeyIndex, node.getMac(), elementAddress, modelId);
        }
    }

    @Override
    public void modelSubscriptionAdd(ModelSubscriptionAddMessage message) {
        mLastSubscriptionOp = SubscriptionOp.Add;
        postMessage(message);
    }

    @Override
    public void modelSubscriptionDelete(ModelSubscriptionDeleteMessage message) {
        mLastSubscriptionOp = SubscriptionOp.Delete;
        postMessage(message);
    }

    @Override
    public void modelSubscriptionOverwrite(ModelSubscriptionOverwriteMessage message) {
        mLastSubscriptionOp = SubscriptionOp.Overwrite;
        postMessage(message);
    }

    @Override
    public void modelSubscriptionDeleteAll(ModelSubscriptionDeleteAllMessage message) {
        mLastSubscriptionOp = SubscriptionOp.DeleteAll;
        postMessage(message);
    }

    private void parseModelSubscriptionStatus(byte[] deviceAddr, byte[] payload) {
        Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
        if (node == null) {
            return;
        }

        int status = payload[2] & 0xff;
        long elementAddress = MeshUtils.bigEndianBytesToLong(new byte[]{payload[4], payload[3]});
        long subAddress = MeshUtils.bigEndianBytesToLong(new byte[]{payload[6], payload[5]});
        byte[] modelId = DataUtil.reverseBytes(DataUtil.subBytes(payload, 7));
        String modelIdStr = DataUtil.bigEndianBytesToHexString(modelId);

        if (status == 0) {
            MeshObjectBox dbManager = MeshObjectBox.getInstance();
            switch (mLastSubscriptionOp) {
                case Add: {
                    dbManager.saveModelInGroup(subAddress, node.getMac(), elementAddress, modelIdStr);
                    break;
                }
                case Delete: {
                    dbManager.deleteModelFromGroup(subAddress, node.getMac(), elementAddress, modelIdStr);
                    break;
                }
                case Overwrite: {
                    dbManager.deleteElementFromGroup(subAddress, node.getMac(), elementAddress);
                    dbManager.saveModelInGroup(subAddress, node.getMac(), elementAddress, modelIdStr);
                    break;
                }
                case DeleteAll: {
                    dbManager.deleteElementFromGroup(subAddress, node.getMac(), elementAddress);
                    break;
                }
            }
        }

        if (mMessageCallback != null) {
            mMessageCallback.onModelSubscriptionStatus(status, subAddress, node.getMac(), elementAddress, modelIdStr);
        }
    }

    @Override
    public void relaySet(RelaySetMessage message) {
        postMessage(message);
    }

    private void parseRelayStatus(byte[] payload) {
        int state = payload[2] & 0xff;
        int retransmit = payload[3] & 0xff;
        int count = retransmit & 7;
        int step = retransmit >> 3;
        mLog.d("Parse relay status state = " + state + " , retransmit = " + retransmit);

        if (mMessageCallback != null) {
            mMessageCallback.onRelayStatus(state, count, step);
        }
    }

    @Override
    public void genericOnOff(GenericOnOffMessage message) {
        postMessage(message);
    }

    private void parseGenericOnOffStatus(byte[] payload) {
        int state = payload[2] & 0xff;
    }

    @Override
    public void lightSetHSL(LightHSLSetMessage message) {
        postMessage(message);
    }

    @Override
    public void lightGetHSL(LightHSLGetMessage message) {
        postMessage(message);
    }

    private void parseLightHSLStatus(byte[] payload) {
        int l = (payload[2] & 0xff) | ((payload[3] & 0xff) << 8);
        int h = (payload[4] & 0xff) | ((payload[5] & 0xff) << 8);
        int s = (payload[6] & 0xff) | ((payload[7] & 0xff) << 8);
        int[] lightHSL = {h, s, l};
        int[] rgb = MeshUtils.lightHSLtoRGB(lightHSL);

        if (mMessageCallback != null) {
            mMessageCallback.onLightHSLStatus(rgb);
        }
    }

    @Override
    public void fastProvInfoSet(FastProvInfoSetMessage message) {
        postMessage(message);
    }

    @Override
    public void fastGroupBind(FastGroupBindMessage message) {
        postMessage(message);
    }

    @Override
    public void fastGroupUnbind(FastGroupUnbindMessage message) {
        postMessage(message);
    }

    private void parseFastProvStatus(byte[] deviceAddr, byte[] payload) {
        mLog.i("parseFastProvStatus");
        Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
        if (node == null) {
            return;
        }

        if (mMessageCallback != null) {
            mMessageCallback.onFastProvStatus();
        }
    }

    @Override
    public void fastProvNodeAddrGet(FastProvNodeAddrGetMessage message) {
        postMessage(message);
    }

    private void parseFastProvNodeAddrStatus(byte[] deviceAddr, byte[] payload) {
        mLog.i("parseFastProvNodeAddrStatus");
        Node node = mNodeMap.get(MeshUtils.bigEndianBytesToLong(deviceAddr));
        if (node == null) {
            return;
        }

        byte[] allAddrData = DataUtil.subBytes(payload, 3);
        int addrCount = allAddrData.length / 2;
        long[] addrArray = new long[addrCount];
        for (int i = 0; i < addrCount; i++) {
            int begin = i * 2;
            byte[] addrData = {allAddrData[begin + 1], allAddrData[begin]};
            addrArray[i] = MeshUtils.bigEndianBytesToLong(addrData);
        }

        if (mMessageCallback != null) {
            mMessageCallback.onFastProvNodeAddrStatus(node.getMac(), addrArray);
        }
    }

    public void otaNewBinVersionNotification(OtaNBVNMessage message) {
        postMessage(message);
    }

    private void parseNeedOTAUpdateNotification(byte[] deviceAddr, byte[] payload) {
        // TODO
        byte[] manufacturerId = {payload[1], payload[2]};
        byte[] binId = {payload[3], payload[4]};
        byte[] version = {payload[5], payload[6]};
        if (mMessageCallback != null) {
            mMessageCallback.onNeedOTAUpdateNotification(manufacturerId, binId, version);
        }
    }

    public void otaStart(OtaStartMessage message) {
        postMessage(message);
    }

    private void parseOTAStartResponse(byte[] deviceAddr, byte[] payload) {
        // TODO
        if (mMessageCallback != null) {
            mMessageCallback.onOTAStartResponse();
        }
    }

    private class WriteThread extends Thread {
        @Override
        public void run() {
            while (!mClosed && !interrupted()) {
                try {
                    byte[] data = mPostDataQueue.take();
                    if (!write(data)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    mLog.w("InterruptedException WriteThread");
                    interrupt();
                    break;
                }
            }

            mLog.d("WriteThread over");
        }

        private boolean write(byte[] bytes) {
            synchronized (mWriteLock) {
                if (!mClosed && getWriteChar() != null && getGatt() != null) {
                    getWriteChar().setValue(bytes);
                    getGatt().writeCharacteristic(getWriteChar());

                    try {
                        mWriteLock.wait();
                    } catch (InterruptedException e) {
                        mLog.w("InterruptedException WriteLock");
                        interrupt();
                        return false;
                    }
                }

                return true;
            }
        }
    }

    private class SegmentedMessage {
        byte[] firstSeq;

        List<byte[]> mSegmentedMessageList = new Vector<>();

        void appendSegment(byte[] segment) {
            mSegmentedMessageList.add(segment);
        }

        byte[] getUpperTransportPDU() {
            byte[] result = new byte[0];
            for (byte[] segment : mSegmentedMessageList) {
                result = DataUtil.mergeBytes(result, segment);
            }
            return result;
        }
    }
}
