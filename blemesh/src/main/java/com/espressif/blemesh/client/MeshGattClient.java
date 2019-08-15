package com.espressif.blemesh.client;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.espressif.blemesh.client.callback.MeshGattCallback;
import com.espressif.blemesh.client.abs.PrivateMeshGattClientAbs;
import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.utils.MeshUtils;

import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

public class MeshGattClient extends PrivateMeshGattClientAbs implements IMeshGattClient {
    private final EspLog mLog = new EspLog(getClass());

    private BluetoothDevice mDevice;
    private byte[] mDeviceUUID;
    private byte[] mAppAddr;

    private MeshGattProvisioner mProvisioner;
    private MeshGattMessager mMessager;

    private MeshGattCallback mMeshCallback;

    private int mMTU = MeshConstants.MTU_LENGTH_MIN;

    public MeshGattClient(BluetoothDevice device) {
        mDevice = device;
    }

    @Override
    public void connect(Context context) {
        connectGatt(context);
    }

    @Override
    public void close() {
        closeGatt();
        if (mProvisioner != null) {
            mProvisioner.release();
        }
        if (mMessager != null) {
            mMessager.release();
        }
    }

    @Override
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    @Override
    public void setDeviceUUID(byte[] deviceUUID) {
        mDeviceUUID = deviceUUID;
    }

    @Override
    public byte[] getDeviceUUID() {
        return mDeviceUUID;
    }

    @Override
    public void setAppAddr(long appAddr) {
        mAppAddr = MeshUtils.addressLongToBigEndianBytes(appAddr);
    }

    @Override
    public void setGattCallback(BluetoothGattCallback callback) {
        super.setGattCB(callback);
    }

    @Override
    public void setMeshCallback(MeshGattCallback meshCallback) {
        mMeshCallback = meshCallback;
    }

    protected int getMTU() {
        return mMTU;
    }

    @Override
    public void discoverGattServices() {
        if (getGatt() != null) {
            boolean suc = getGatt().discoverServices();
            mLog.d("discoverGattServices " + suc);
        }
    }

    @Override
    protected void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            if (!gatt.requestMtu(MeshConstants.MTU_LENGTH_DEFAULT)) {
                gatt.discoverServices();
            }
        }
    }

    @Override
    protected void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mMTU = mtu;
        }
        gatt.discoverServices();
    }

    @Override
    protected void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect();;
            return;
        }

        MeshGattProvisioner provisioner = parseProvisionerService(gatt);
        if (provisioner != null) {
            provisioner.setGattMTU(mMTU);
            if (mProvisioner == null) {
                mProvisioner = provisioner;
                mProvisioner.getNotifyDesc().setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(mProvisioner.getNotifyDesc());
            }
        } else {
            if (mProvisioner != null) {
                mProvisioner.release();
                mProvisioner = null;
            }
        }

        MeshGattMessager messager = parseMessageService(gatt);
        if (messager != null) {
            messager.setGattMTU(mMTU);
            if (mMessager == null) {
                mMessager = messager;
                mMessager.getNotifyDesc().setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(mMessager.getNotifyDesc());
            }
        } else {
            if (mMessager != null) {
                mMessager.release();
            }
        }
    }

    @Override
    protected void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect();
            return;
        }

        if (mMeshCallback != null) {
            if (mProvisioner != null && mProvisioner.getNotifyDesc() == descriptor) {
                mMeshCallback.onDiscoverDeviceServiceResult(MeshGattCallback.CODE_SUCCESS, mProvisioner);
            }
            if (mMessager != null && mMessager.getNotifyDesc() == descriptor) {
                mMeshCallback.onDiscoverNodeServiceResult(MeshGattCallback.CODE_SUCCESS, mMessager);
            }
        }
    }

    @Override
    protected void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        if (DataUtil.isEmpty(data)) {
            mLog.w("onCharacteristicChanged empty data");
            return;
        } else {
            mLog.d("onCharacteristicChanged " + data.length);
        }

        int proxyType = data[0] & 0xff;
        int proxy = proxyType >> 6;
        int type = proxyType & 63;
        switch (type) {
            case MeshConstants.PROXY_TYPE_PROVISIONING_PDU:
                mLog.d("onCharacteristicChanged PROVISIONING_PDU");
                if (mProvisioner != null && characteristic == mProvisioner.getNotifyChar()) {
                    mProvisioner.onNotification(data);
                }
                break;
            case MeshConstants.PROXY_TYPE_NETWORK_PDU:
                mLog.d("onCharacteristicChanged NETWORK_PDU");
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION:
                if (mMessager != null && characteristic == mMessager.getNotifyChar()) {
                    mMessager.onNotification(data);
                }
                break;
            default:
                mLog.d("onCharacteristicChanged unknown Type " + type);
                mLog.d(DataUtil.bigEndianBytesToHexString(data));
                break;
        }
    }

    @Override
    protected void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect();
            return;
        }

        byte[] data = characteristic.getValue();
        int proxyType = data[0] & 0xff;
        int proxy = proxyType >> 6;
        int type = proxyType & 63;
        switch (type) {
            case MeshConstants.PROXY_TYPE_PROVISIONING_PDU:
                if (mProvisioner != null && characteristic == mProvisioner.getWriteChar()) {
                    mProvisioner.onWrote(data);
                }
                break;
            case MeshConstants.PROXY_TYPE_NETWORK_PDU:
            case MeshConstants.PROXY_TYPE_PROXY_CONGURATION:
                if (mMessager != null && characteristic == mMessager.getWriteChar()) {
                    mMessager.onWrote(data);
                }
                break;
        }
    }

    private MeshGattProvisioner parseProvisionerService(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(MeshConstants.UUID_DEVICE_SERVICE);
        if (service == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverDeviceServiceResult( MeshGattCallback.CODE_ERR_SERVICE, null);
            }
            return null;
        }

        BluetoothGattCharacteristic writeChar = service.getCharacteristic(MeshConstants.UUID_DEVICE_CHAR_WRITE);
        if (writeChar == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverDeviceServiceResult(MeshGattCallback.CODE_ERR_WRITE_CHAR, null);
            }
            return null;
        }

        BluetoothGattCharacteristic notifyChar = service.getCharacteristic(MeshConstants.UUID_DEVICE_CHAR_NOTIFICATION);
        if (notifyChar == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverDeviceServiceResult(MeshGattCallback.CODE_ERR_NOTIFY_CHAR, null);
            }
            return null;
        }
        gatt.setCharacteristicNotification(notifyChar, true);

        BluetoothGattDescriptor notifyDesc = notifyChar.getDescriptor(MeshConstants.UUID_DESC_NOTIFICATION);
        if (notifyDesc == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverDeviceServiceResult(MeshGattCallback.CODE_ERR_NOTIFY_DESC, null);
            }
            return null;
        }

        MeshGattProvisioner provisioner = new MeshGattProvisioner();
        provisioner.setGatt(gatt);
        provisioner.setService(service);
        provisioner.setWriteChar(writeChar);
        provisioner.setNotifyChar(notifyChar);
        provisioner.setNotifyDesc(notifyDesc);
        provisioner.setDeviceUUID(mDeviceUUID);
        provisioner.setDevice(mDevice);

        return provisioner;
    }

    private MeshGattMessager parseMessageService(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(MeshConstants.UUID_NODE_SERVICE);
        if (service == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverNodeServiceResult(MeshGattCallback.CODE_ERR_SERVICE, null);
            }
            return null;
        }

        BluetoothGattCharacteristic writeChar = service.getCharacteristic(MeshConstants.UUID_NODE_CHAR_WRITE);
        if (writeChar == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverNodeServiceResult( MeshGattCallback.CODE_ERR_WRITE_CHAR, null);
            }
            return null;
        }

        BluetoothGattCharacteristic notifyChar = service.getCharacteristic(MeshConstants.UUID_NODE_CHAR_NOTIFICATION);
        if (notifyChar == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverNodeServiceResult(MeshGattCallback.CODE_ERR_NOTIFY_CHAR, null);
            }
            return null;
        }
        gatt.setCharacteristicNotification(notifyChar, true);

        BluetoothGattDescriptor notifyDesc = notifyChar.getDescriptor(MeshConstants.UUID_DESC_NOTIFICATION);
        if (notifyDesc == null) {
            if (mMeshCallback != null) {
                mMeshCallback.onDiscoverNodeServiceResult(MeshGattCallback.CODE_ERR_NOTIFY_DESC, null);
            }
            return null;
        }

        MeshGattMessager messager = new MeshGattMessager();
        messager.setGatt(gatt);
        messager.setService(service);
        messager.setWriteChar(writeChar);
        messager.setNotifyChar(notifyChar);
        messager.setNotifyDesc(notifyDesc);
        messager.setDeviceUUID(mDeviceUUID);
        messager.setDevice(mDevice);
        messager.setAppAddr(mAppAddr);

        return messager;
    }
}
