package com.espressif.blemesh.client.callback;

import com.espressif.blemesh.model.Node;

public abstract class ProvisioningCallback {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_FAILED = -1;

    public void onProvisioningFailed(int code) {
    }

    public void onProvisioningSuccess(int code, Node node) {
    }
}
