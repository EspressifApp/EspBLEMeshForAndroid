package com.espressif.espblemesh.eventbus.blemesh;

public class LightHSLEvent {
    private int mRed;
    private int mGreen;
    private int mBlue;

    public LightHSLEvent(int red, int green, int blue) {
        mRed = red;
        mGreen = green;
        mBlue = blue;
    }

    public int getRed() {
        return mRed;
    }

    public int getGreen() {
        return mGreen;
    }

    public int getBlue() {
        return mBlue;
    }
}
