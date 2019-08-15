package com.espressif.blemesh.client;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import com.espressif.blemesh.client.callback.MeshGattCallback;

public interface IMeshGattClient {
    void connect(Context context);

    void close();

    void discoverGattServices();

    BluetoothDevice getDevice();

    void setDeviceUUID(byte[] deviceUUID);

    byte[] getDeviceUUID();

    void setAppAddr(long appAddr);

    void setGattCallback(BluetoothGattCallback callback);

    void setMeshCallback(MeshGattCallback meshCallback);
}
