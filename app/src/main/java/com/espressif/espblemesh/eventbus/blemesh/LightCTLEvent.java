package com.espressif.espblemesh.eventbus.blemesh;

import androidx.annotation.NonNull;

import java.util.Locale;

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

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "LightCTLEvent >> Lightness:%d, Temperature:%d, DeltaUV:%d",
                mLightness, mTemperature, mDeltaUV);
    }
}
