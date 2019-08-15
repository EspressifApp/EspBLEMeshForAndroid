package com.espressif.espblemesh.ui.network.ota;

import android.bluetooth.BluetoothGattCallback;

import com.espressif.blemesh.client.IMeshMessager;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.ui.network.SimpleMessageCallback;

public class OTAPackage {
    public Node node;
    public IMeshMessager messager;
    public SimpleMessageCallback messageCB;
    public BluetoothGattCallback gattCB;
}
