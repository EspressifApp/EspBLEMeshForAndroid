package com.espressif.blemesh.utils;

import com.espressif.blemesh.constants.MeshConstants;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.CCMParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

import libs.espressif.security.EspAES;
import libs.espressif.utils.DataUtil;

public class MeshAlgorithmUtils {
    public static BigInteger generateECDHSecret(BigInteger appPrivateKey, BigInteger devPublicKeyX,
                                                BigInteger devPublicKeyY) {
        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECCurve curve = ecParameterSpec.getCurve();
        ECPoint devPoint = curve.createPoint(devPublicKeyX, devPublicKeyY, true);
        ECPoint p256Point = devPoint.multiply(appPrivateKey);
        return p256Point.getX().toBigInteger();
    }

    public static byte[] AES_CMAC(byte[] key16, byte[] data) {
        CipherParameters parameters = new KeyParameter(key16);
        BlockCipher aes = new AESEngine();
        CMac cMac = new CMac(aes, 128);
        cMac.init(parameters);
        cMac.update(data, 0, data.length);
        byte[] result = new byte[16];
        cMac.doFinal(result, 0);

        return result;
    }

    public static byte[] AES_CCM_Encrypt(byte[] key, byte[] nonce, int micBitSize, byte[] data) {
        KeyParameter keyParameter = new KeyParameter(key);
        CCMParameters parameters = new CCMParameters(keyParameter, micBitSize, nonce, null);
        BlockCipher aes = new AESEngine();
        CCMBlockCipher cipher = new CCMBlockCipher(aes);
        cipher.init(true, parameters);
        cipher.processBytes(data, 0, data.length, null, 0);
        int micByteSize = micBitSize / 8;
        if(micBitSize % 8 > 0) {
            micByteSize += 1;
        }
        byte[] out = new byte[data.length + micByteSize];
        try {
            cipher.doFinal(out, 0);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }

        return out;
    }

    public static byte[] AES_CCM_Decrypt(byte[] key, byte[] nonce, int micBitSize, byte[] data) {
        KeyParameter keyParameter = new KeyParameter(key);
        CCMParameters parameters = new CCMParameters(keyParameter, micBitSize, nonce, null);
        BlockCipher aes = new AESEngine();
        CCMBlockCipher cipher = new CCMBlockCipher(aes);
        cipher.init(false, parameters);
        cipher.processBytes(data, 0, data.length, null, 0);
        int micByteSize = micBitSize / 8;
        if(micBitSize % 8 > 0) {
            micByteSize += 1;
        }
        byte[] out = new byte[data.length - micByteSize];
        try {
            cipher.doFinal(out, 0);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }

        return out;
    }

    public static byte[] e(byte[] key, byte[] data) {
        EspAES aes = new EspAES(key, "AES/ECB/PKCS5Padding");
        return aes.encrypt(data);
    }

    public static byte[] s1(byte[] M) {
        byte[] key = new byte[16];
        return AES_CMAC(key, M);
    }

    public static byte[] k1(byte[] N, byte[] salt, byte[] P) {
        byte[] T = AES_CMAC(salt, N);
        return AES_CMAC(T, P);
    }

    public static byte[] k2(byte[] N, byte[] P) {
        byte[] salt = s1(MeshConstants.BYTES_SMK2);
        byte[] T = AES_CMAC(salt, N);
        byte[] T0 = new byte[0];
        byte[] T1 = AES_CMAC(T, DataUtil.mergeBytes(T0, P, new byte[]{0x01}));
        byte[] T2 = AES_CMAC(T, DataUtil.mergeBytes(T1, P, new byte[]{0x02}));
        byte[] T3 = AES_CMAC(T, DataUtil.mergeBytes(T2, P, new byte[]{0x03}));

        BigInteger T123 = new BigInteger(DataUtil.mergeBytes(T1, T2, T3));
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 263; i++) {
            sb.append("0");
        }
        BigInteger m = new BigInteger(sb.toString(), 2);
        BigInteger mod = T123.mod(m);
        return DataUtil.hexStringToBigEndianBytes(mod.toString(16));
    }

    public static byte[] k3(byte[] N) {
        byte[] salt = s1(MeshConstants.BYTES_SMK3);
        byte[] T = AES_CMAC(salt, N);
        byte[] tempData = AES_CMAC(T, DataUtil.mergeBytes(MeshConstants.BYTES_ID64, new byte[]{0x01}));
        BigInteger tempInteger = new BigInteger(tempData);
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 64; i++) {
            sb.append("0");
        }
        BigInteger m = new BigInteger(sb.toString(), 2);
        BigInteger mod = tempInteger.mod(m);
        return DataUtil.hexStringToBigEndianBytes(mod.toString(16));
    }

    public static byte[] k4(byte[] N) {
        byte[] salt = s1(MeshConstants.BYTES_SMK4);
        byte[] T = AES_CMAC(salt, N);
        byte[] tempData = AES_CMAC(T, DataUtil.mergeBytes(MeshConstants.BYTES_ID6, new byte[]{0x01}));
        BigInteger tempInteger = new BigInteger(tempData);
        BigInteger m = new BigInteger("1000000", 2);
        BigInteger mod = tempInteger.mod(m);
        return DataUtil.hexStringToBigEndianBytes(mod.toString(16));
    }
}
