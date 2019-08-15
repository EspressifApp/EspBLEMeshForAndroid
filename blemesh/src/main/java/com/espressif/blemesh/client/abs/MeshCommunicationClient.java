package com.espressif.blemesh.client.abs;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.espressif.blemesh.constants.MeshConstants;

public abstract class MeshCommunicationClient {
    private BluetoothDevice mDevice;
    private byte[] mDeviceUUID;

    private BluetoothGatt mGatt;
    private int mGattMTU = MeshConstants.MTU_LENGTH_MIN;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mWriteChar;
    private BluetoothGattCharacteristic mNotifyChar;
    private BluetoothGattDescriptor mNotifyDesc;

    public void release() {
        mDevice = null;
        mDeviceUUID = null;
        mGatt = null;
        mService = null;
        mWriteChar = null;
        mNotifyChar = null;
        mNotifyDesc = null;
    }

    public void setDevice(BluetoothDevice device) {
        mDevice = device;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void setGatt(BluetoothGatt gatt) {
        mGatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public void setGattMTU(int mtu) {
        mGattMTU = mtu;
    }

    protected int getAvailableGattMTU() {
        return mGattMTU - 3;
    }

    public void setService(BluetoothGattService service) {
        mService = service;
    }

    public BluetoothGattService getService() {
        return mService;
    }

    public void setWriteChar(BluetoothGattCharacteristic writeChar) {
        mWriteChar = writeChar;
    }

    public BluetoothGattCharacteristic getWriteChar() {
        return mWriteChar;
    }

    public void setNotifyChar(BluetoothGattCharacteristic notifyChar) {
        mNotifyChar = notifyChar;
    }

    public BluetoothGattCharacteristic getNotifyChar() {
        return mNotifyChar;
    }

    public void setNotifyDesc(BluetoothGattDescriptor notifyDesc) {
        mNotifyDesc = notifyDesc;
    }

    public BluetoothGattDescriptor getNotifyDesc() {
        return mNotifyDesc;
    }

    public void setDeviceUUID(byte[] deviceUUID) {
        mDeviceUUID = deviceUUID;
    }

    public byte[] getDeviceUUID() {
        return mDeviceUUID;
    }

    public abstract void onNotification(byte[] data);

    public abstract void onWrote(byte[] data);
}
