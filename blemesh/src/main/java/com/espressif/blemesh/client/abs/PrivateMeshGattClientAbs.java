package com.espressif.blemesh.client.abs;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Build;

import libs.espressif.ble.EspBleUtils;

public abstract class PrivateMeshGattClientAbs {
    private final Object mGattLock = new Object();
    private BluetoothGatt mGatt;
    private BluetoothGattCallback mGattCallback;

    public abstract BluetoothDevice getDevice();

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    protected void connectGatt(Context context) {
        closeGatt();

        synchronized (mGattLock) {
            mGatt = EspBleUtils.connectGatt(getDevice(), context, new MeshGattCallback());
        }
    }

    protected void closeGatt() {
        synchronized (mGattLock) {
            if (mGatt != null) {
                mGatt.close();
                mGatt = null;
            }
        }
    }

    protected void setGattCB(BluetoothGattCallback callback) {
        mGattCallback = callback;
    }

    protected abstract void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);
    protected abstract void onMtuChanged(BluetoothGatt gatt, int mtu, int status);
    protected abstract void onServicesDiscovered(BluetoothGatt gatt, int status);
    protected abstract void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);
    protected abstract void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    protected abstract void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    private class MeshGattCallback extends BluetoothGattCallback {
        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            if (mGattCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mGattCallback.onPhyRead(gatt, txPhy, rxPhy, status);
                }
            }
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            if (mGattCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mGattCallback.onPhyUpdate(gatt, txPhy, rxPhy, status);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            PrivateMeshGattClientAbs.this.onConnectionStateChange(gatt, status, newState);

            if (mGattCallback != null) {
                mGattCallback.onConnectionStateChange(gatt, status, newState);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            PrivateMeshGattClientAbs.this.onMtuChanged(gatt, mtu, status);

            if (mGattCallback != null) {
                mGattCallback.onMtuChanged(gatt, mtu, status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            PrivateMeshGattClientAbs.this.onServicesDiscovered(gatt, status);

            if (mGattCallback != null) {
                mGattCallback.onServicesDiscovered(gatt, status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (mGattCallback != null) {
                mGattCallback.onReadRemoteRssi(gatt, rssi, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            PrivateMeshGattClientAbs.this.onCharacteristicChanged(gatt, characteristic);

            if (mGattCallback != null) {
                mGattCallback.onCharacteristicChanged(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            PrivateMeshGattClientAbs.this.onCharacteristicWrite(gatt, characteristic, status);

            if (mGattCallback != null) {
                mGattCallback.onCharacteristicWrite(gatt, characteristic, status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            if (mGattCallback != null) {
                mGattCallback.onReliableWriteCompleted(gatt, status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mGattCallback != null) {
                mGattCallback.onCharacteristicRead(gatt, characteristic, status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            PrivateMeshGattClientAbs.this.onDescriptorWrite(gatt, descriptor, status);

            if (mGattCallback != null) {
                mGattCallback.onDescriptorWrite(gatt, descriptor, status);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (mGattCallback != null) {
                mGattCallback.onDescriptorRead(gatt, descriptor, status);
            }
        }
    }
}
