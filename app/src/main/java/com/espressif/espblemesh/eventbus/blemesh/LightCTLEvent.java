package com.espressif.espblemesh.eventbus.blemesh;

public class LightCTLEvent {
    private int mLightness;
    private int mTemperature;
    private int mDeltaUV;

    public LightCTLEvent(int lightness, int temperature, int deltaUV) {
        mLightness = lightness;
        mTemperature = temperature;
        mDeltaUV = deltaUV;
    }

    public int getLightness() {
        return mLightness;
    }

    public int getTemperature() {
        return mTemperature;
    }

    public int getDeltaUV() {
        return mDeltaUV;
    }
}
