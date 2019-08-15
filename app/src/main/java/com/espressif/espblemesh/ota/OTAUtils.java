package com.espressif.espblemesh.ota;

import com.espressif.blemesh.utils.MeshUtils;

public class OTAUtils {
    public static String getSoftApSSID(String ssidInput, long nodeAddr) {
        if (ssidInput.getBytes().length != 2) {
            throw new IllegalArgumentException("SSID ssidInput bytes length must be 2");
        }

        StringBuilder sb = new StringBuilder(ssidInput);
        byte[] addrBytes = MeshUtils.addressLongToBigEndianBytes(nodeAddr);
        for (byte b : addrBytes) {
            sb.append(getSoftApCharForByte(b));
        }
        return sb.toString();
    }

    public static String getSoftApPassword(String passworInput, String ssidInput, long nodeAddr) {
        if (passworInput.getBytes().length!= 2) {
            throw new IllegalArgumentException("Password input bytes length must be 2");
        }
        if (ssidInput.getBytes().length != 2) {
            throw new IllegalArgumentException("SSID input bytes length must be 2");
        }

        StringBuilder sb = new StringBuilder(passworInput);
        sb.append(ssidInput);
        byte[] addrBytes = MeshUtils.addressLongToBigEndianBytes(nodeAddr);
        for (byte b : addrBytes) {
            sb.append(getSoftApCharForByte(b));
        }
        sb.append(passworInput);

        return sb.toString();
    }

    private static char getSoftApCharForByte(byte b) {
        int i = b & 0xff;
        if (i >= 0x21 && i <= 0x7E) {
            return (char) i;
        } else {
            return (char) (i % 26 + 0x61);
        }
    }
}
