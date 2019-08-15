package com.espressif.blemesh.client.callback;

import com.espressif.blemesh.client.IMeshMessager;
import com.espressif.blemesh.client.IMeshProvisioner;

public abstract class MeshGattCallback {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_FAILED = -1;

    public static final int CODE_ERR_SERVICE = -10;
    public static final int CODE_ERR_WRITE_CHAR = -11;
    public static final int CODE_ERR_NOTIFY_CHAR = -12;
    public static final int CODE_ERR_NOTIFY_DESC = -13;

    public void onDiscoverDeviceServiceResult(int code, IMeshProvisioner provisioner) {
    }

    public void onDiscoverNodeServiceResult(int code, IMeshMessager messager) {
    }
}
