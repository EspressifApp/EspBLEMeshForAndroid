package com.espressif.espblemesh.ui;

import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.utils.MeshAlgorithmUtils;

import java.math.BigInteger;

import libs.espressif.utils.DataUtil;

public class Test {
    public void test() {
        String netKeyStr = "d1aafb2a1a3c281cbdb0e960edfad852";
        Network network = new Network(DataUtil.hexStringToBigEndianBytes(netKeyStr),
                0, "net", 0x12345678, 1);
        System.out.println("XXJTEST nid = " + network.getNid());
        System.out.println("XXJTEST enK = " + DataUtil.bigEndianBytesToHexString(network.getEncryptionKey()));
        System.out.println("XXJTEST prK = " + DataUtil.bigEndianBytesToHexString(network.getPrivacyKey()));

        byte[] nonce = getProxyNonce(new byte[]{0, 0, 1}, new byte[]{0, 1}, new byte[]{0x12, 0x34, 0x56, 0x78});
        System.out.println("XXJTEST nonce = " + DataUtil.bigEndianBytesToHexString(nonce));

        byte[] d = MeshAlgorithmUtils.AES_CCM_Encrypt(network.getEncryptionKey(), nonce, 32,
                new byte[]{0, 0});
        System.out.println("XXJTEST " + DataUtil.bigEndianBytesToHexString(d));

        byte[] head = getUnsegmentedHeader(0);


        byte[] netNonce = getNetNonce(1, 0,
                new byte[]{0, 0, 1}, new byte[]{0 ,1}, network.getIVIndexBytes());

        d = new byte[]{0, 0};
        int ctl = 1;
        int ttl = 0;
        int micBitSize = ctl == 0 ? 32 : 64;
        byte[] encTransport = MeshAlgorithmUtils.AES_CCM_Encrypt(network.getEncryptionKey(), nonce, micBitSize,
                DataUtil.mergeBytes(new byte[]{0, 0}, d));
        System.out.println("XXJ ENCT = " + DataUtil.bigEndianBytesToHexString(encTransport));

        byte[] privacyRandom = new byte[7];
        System.arraycopy(encTransport, 0, privacyRandom, 0, 7);
        byte[] pecb = MeshAlgorithmUtils.e(network.getPrivacyKey(),
                DataUtil.mergeBytes(new byte[5], network.getIVIndexBytes(), privacyRandom));
        byte[] pecb05 = new byte[6];
        System.arraycopy(pecb, 0, pecb05, 0, 6);
        byte[] obfuscatedData = new BigInteger(DataUtil.mergeBytes(new byte[]{getCtlTtl(ctl, ttl)},
                new byte[]{0, 0, 1}, new byte[]{0 ,1}))
                .xor(new BigInteger(pecb05))
                .toByteArray();
        byte[] result =  DataUtil.mergeBytes(new byte[]{network.getNid()}, obfuscatedData, encTransport);
        System.out.println("XXJTEST res = " + DataUtil.bigEndianBytesToHexString(result));

        // 8b8c28512e792d3711f4b526
        // 8b8c28512e792d3711f4b526

        // 10386bd60efbbb8b8c28512e792d3711f4b526
        // 10386bd60efbbb8b8c28512e792d3711f4b526
    }

//    private byte[] getNetworkPDU(byte[] lowerTransportPDU, int ctl, int ttl, long seq, byte[] dst) {
//        byte[] netNonce = getNetNonce(ctl, ttl, MeshUtils.getSequenceBytes(seq), mAppAddr, mNetwork.getIVIndexBytes());
//
//        int micBitSize = ctl == 0 ? 32 : 64;
//        byte[] encTransport = MeshAlgorithmUtils.AES_CCM_Encrypt(mNetwork.getEncryptionKey(), netNonce, micBitSize,
//                DataUtil.mergeBytes(dst, lowerTransportPDU));
//
//        byte[] privacyRandom = new byte[7];
//        System.arraycopy(encTransport, 0, privacyRandom, 0, 7);
//        byte[] pecb = MeshAlgorithmUtils.e(mNetwork.getPrivacyKey(),
//                DataUtil.mergeBytes(new byte[5], mNetwork.getIVIndexBytes(), privacyRandom));
//        byte[] pecb05 = new byte[6];
//        System.arraycopy(pecb, 0, pecb05, 0, 6);
//        byte[] obfuscatedData = new BigInteger(DataUtil.mergeBytes(new byte[]{getCtlTtl(ctl, ttl)},
//                MeshUtils.getSequenceBytes(seq), mAppAddr))
//                .xor(new BigInteger(pecb05))
//                .toByteArray();
//        return DataUtil.mergeBytes(new byte[]{mNetwork.getNid()}, obfuscatedData, encTransport);
//    }

    private byte[] getUnsegmentedHeader(int aid) {
        long seg = 0;
        long akf = aid == 0 ? 0 : 1;
        String aidBitString = getAidBitString(aid);
        int i = Integer.parseInt(seg + akf + aidBitString, 2);
        return new byte[]{(byte) i};
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

    private byte getCtlTtl(int ctl, int ttl) {
        return (byte) ((ctl << 7 & 0xff) | (ttl & 0x7f));
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
}
