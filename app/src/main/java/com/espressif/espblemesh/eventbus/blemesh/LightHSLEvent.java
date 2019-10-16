package com.espressif.espblemesh.eventbus.blemesh;

import androidx.annotation.NonNull;

import java.util.Locale;

public class LightHSLEvent {
    private float mHue;
    private float mSaturation;
    private float mLightness;

    public LightHSLEvent(float hue, float saturation, float lightness) {
        mHue = hue;
        mSaturation = saturation;
        mLightness = lightness;
    }

    public float getHue() {
        return mHue;
    }

    public float getSaturation() {
        return mSaturation;
    }

    public float getLightness() {
        return mLightness;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "LightHSLEvent >> Hue:%f, Saturation:%f, Lightness:%f",
                mHue, mSaturation, mLightness);
    }
}
