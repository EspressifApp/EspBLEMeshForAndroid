package com.espressif.blemesh.client.callback;

public abstract class MessageCallback {
    public static final int CODE_SUCCESS = 0;

    public void onAppKeyStatus(int status, long netKeyIndex, long appKeyIndex) {
    }

    public void onCompositionDataStatus(int status, int page) {
    }

    public void onModelAppStatus(int status, long appKeyIndex, String nodeMac, long elementAddr, String modeId) {
    }

    public void onModelSubscriptionStatus(int status, long groupAddr, String nodeMac, long elementAddr, String modelId) {
    }

    public void onRelayStatus(int state, int count, int step) {
    }

    public void onLightHSLStatus(int[] rgb) {
    }

    public void onLightCTLStatus(int lightness, int temperature, int deltaUV) {
    }

    public void onFastProvStatus() {
    }

    public void onFastProvNodeAddrStatus(String nodeMac, long[] addrArray) {
    }

    public void onNeedOTAUpdateNotification(byte[] manufacturerId, byte[] binId, byte[] version) {
    }

    public void onOTAStartResponse() {
    }
}
