package com.espressif.espblemesh.model;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothReceiver extends BroadcastReceiver {
    public static final String ACTION_STATE_CHANGED = BluetoothAdapter.ACTION_STATE_CHANGED;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_STATE_CHANGED: {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        onStateEnable(true);
                        break;
                    default:
                        onStateEnable(false);
                        break;
                }
                break;
            }

        }
    }

    public void onStateEnable(boolean enable) {
    }
}
