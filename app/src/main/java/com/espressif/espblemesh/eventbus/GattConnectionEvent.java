package com.espressif.espblemesh.eventbus;

import android.bluetooth.BluetoothGatt;

public class GattConnectionEvent {
    private BluetoothGatt mGatt;
    private int mStatus;
    private int mState;

    public GattConnectionEvent(BluetoothGatt gatt, int status, int state) {
        mGatt = gatt;
        mStatus = status;
        mState = state;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public int getState() {
        return mState;
    }

    public int getStatus() {
        return mStatus;
    }

    public boolean isConnected() {
        return mStatus == BluetoothGatt.GATT_SUCCESS && mState == BluetoothGatt.STATE_CONNECTED;
    }
}
