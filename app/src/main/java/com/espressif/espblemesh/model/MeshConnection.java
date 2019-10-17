package com.espressif.espblemesh.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanResult;

import androidx.core.graphics.ColorUtils;

import com.espressif.blemesh.client.IMeshMessager;
import com.espressif.blemesh.client.MeshGattClient;
import com.espressif.blemesh.client.callback.MeshGattCallback;
import com.espressif.blemesh.client.callback.MessageCallback;
import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.custom.FastGroupBindMessage;
import com.espressif.blemesh.model.message.custom.FastGroupUnbindMessage;
import com.espressif.blemesh.model.message.custom.FastProvInfoSetMessage;
import com.espressif.blemesh.model.message.custom.FastProvNodeAddrGetMessage;
import com.espressif.blemesh.model.message.standard.AppKeyAddMessage;
import com.espressif.blemesh.model.message.standard.CompositionDataGetMessage;
import com.espressif.blemesh.model.message.standard.GenericLevelSetMessage;
import com.espressif.blemesh.model.message.standard.GenericOnOffSetMessage;
import com.espressif.blemesh.model.message.standard.LightCTLGetMessage;
import com.espressif.blemesh.model.message.standard.LightCTLSetMessage;
import com.espressif.blemesh.model.message.standard.LightHSLGetMessage;
import com.espressif.blemesh.model.message.standard.LightHSLSetMessage;
import com.espressif.blemesh.model.message.standard.ModelAppBindMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionAddMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionDeleteMessage;
import com.espressif.blemesh.model.message.standard.NodeResetMessage;
import com.espressif.blemesh.model.message.standard.RelaySetMessage;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.eventbus.GattNodeServiceEvent;
import com.espressif.espblemesh.eventbus.blemesh.CompositionDataEvent;
import com.espressif.espblemesh.eventbus.blemesh.FastProvAddrEvent;
import com.espressif.espblemesh.eventbus.blemesh.LightCTLEvent;
import com.espressif.espblemesh.eventbus.blemesh.LightHSLEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelAppEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelSubscriptionEvent;
import com.espressif.espblemesh.eventbus.blemesh.NodeResetEvent;
import com.espressif.espblemesh.eventbus.blemesh.RelayEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.Collection;

import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

public enum MeshConnection {
    Instance;

    private final EspLog mLog = new EspLog(MeshConnection.class);

    private MeshUser mUser = MeshUser.Instance;

    private App mApp;

    private MeshGattClient mGattClient;
    private IMeshMessager mMessager;

    private boolean mConnected;

    private Network mNetwork;

    private int mMessagePostCount;

    public void setMessagePostCount(int messagePostCount) {
        mMessagePostCount = messagePostCount;
    }

    public void setApp(App app) {
        mApp = app;
    }

    public App getApp() {
        return mApp;
    }

    public void setNetwork(Network network) {
        mNetwork = network;
    }

    public Network getNetwork() {
        return mNetwork;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public void connectNode(ScanResult scanResult) {
        disconnectNode();

        mGattClient = new MeshGattClient(scanResult.getDevice());
        mGattClient.setMeshCallback(new MeshGattCallback() {
            @Override
            public void onDiscoverNodeServiceResult(int code, IMeshMessager messager) {
                mLog.d("onDiscoverNodeServiceResult " + code);
                if (code == CODE_SUCCESS) {
                    mMessager = messager;
                    mMessager.setNetwork(mNetwork);
                    mMessager.setMessageCallback(new MessageCB());
                }

                GattNodeServiceEvent event = new GattNodeServiceEvent(code, messager);
                EventBus.getDefault().post(event);
            }
        });
        mGattClient.setGattCallback(new GattCallback());
        mGattClient.setAppAddr(mApp.getUnicastAddr());
        mGattClient.connect(MeshApp.getInstance().getApplicationContext());
    }

    public void disconnectNode() {
        if (mGattClient != null) {
            mGattClient.close();
            mGattClient = null;
            mMessager = null;
        }
        mConnected = false;
    }

    public String getConnectedAddress() {
        return isConnected() ? mGattClient.getDevice().getAddress() : null;
    }

    public Node getConnectedNode() {
        if (mGattClient != null) {
            return mUser.getNodeForMac(mGattClient.getDevice().getAddress());
        }
        return null;
    }

    public IMeshMessager getMessager() {
        return mMessager;
    }

    public void genericOnOff(boolean on, Node node, long addr) {
        if (mMessager != null) {
            GenericOnOffSetMessage message = new GenericOnOffSetMessage(addr, node, mApp, on, true);
            message.setPostCount(mMessagePostCount);
            mMessager.genericOnOffSet(message);
        }
    }

    public void genericLevel(int level, Node node, long addr) {
        if (mMessager != null) {
            GenericLevelSetMessage message = new GenericLevelSetMessage(addr, node, mApp, level, true);
            message.setPostCount(mMessagePostCount);
            mMessager.genericLevelSet(message);
        }
    }

    public void bindFastGroup(Node node, long dstAddr, long groupAddr, Collection<Long> nodeAddrs) {
        if (mMessager != null) {
            FastGroupBindMessage message = new FastGroupBindMessage(dstAddr, node, mApp);
            message.setGroupAddr(groupAddr);
            message.setNodeAddrList(nodeAddrs);
            mMessager.fastGroupBind(message);
        }
    }

    public void unbindFastGroup(Node node, long dstAddr, long groupAddr, Collection<Long> nodeAddrs) {
        if (mMessager != null) {
            FastGroupUnbindMessage message = new FastGroupUnbindMessage(dstAddr, node, mApp);
            message.setGroupAddr(groupAddr);
            message.setNodeAddrList(nodeAddrs);
            mMessager.fastGroupUnbind(message);
        }
    }

    public void appKeyAdd(Node node) {
        if (mMessager != null) {
            AppKeyAddMessage message = new AppKeyAddMessage(node, mApp.getAppKey(),
                    mApp.getKeyIndex(), mNetwork.getKeyIndex());
            message.setPostCount(mMessagePostCount);
            mMessager.appKeyAdd(message);
        }
    }

    public void modelAppBind(Node node, Model model) {
        if (mMessager != null) {
            ModelAppBindMessage message = new ModelAppBindMessage(node, model, mApp.getKeyIndex());
            message.setPostCount(mMessagePostCount);
            mMessager.modelAppBind(message);
        }
    }

    public void modelSubscriptionAdd(Node node, Model model, long addr) {
        if (mMessager != null) {
            ModelSubscriptionAddMessage message = new ModelSubscriptionAddMessage(node, model, addr);
            message.setPostCount(mMessagePostCount);
            mMessager.modelSubscriptionAdd(message);
        }
    }

    public void modelSubscriptionDelete(Node node, Model model, long addr) {
        if (mMessager != null) {
            ModelSubscriptionDeleteMessage message = new ModelSubscriptionDeleteMessage(node, model, addr);
            message.setPostCount(mMessagePostCount);
            mMessager.modelSubscriptionDelete(message);
        }
    }

    public void nodeReset(Node node) {
        if (mMessager != null) {
            NodeResetMessage message = new NodeResetMessage(node, node.getUnicastAddress());
            message.setPostCount(mMessagePostCount);
            mMessager.nodeReset(message);
        }
    }

    public void fastProvNodeAddrGet(Node node) {
        if (mMessager != null) {
            FastProvNodeAddrGetMessage message = new FastProvNodeAddrGetMessage(node, mApp);
            mMessager.fastProvNodeAddrGet(message);
        }
    }

    public void fastProv(Node node) {
        if (mMessager != null) {
            FastProvInfoSetMessage message = new FastProvInfoSetMessage(node, mApp);
            message.setProvCount(100);
            message.setUnicastAddressMin(0x0400L);
            message.setMatchValue(DataUtil.hexStringToBigEndianBytes(node.getUUID().substring(0, 4)));
            message.setPrimaryProvisionerAddress(mApp.getUnicastAddr());
            message.setGroupAddress(MeshConstants.ADDRESS_GROUP_MIN);
            message.setAction(((1 << 7) | 1));
            message.setPostCount(mMessagePostCount);
            mMessager.fastProvInfoSet(message);
        }
    }

    public void relaySet(Node node, boolean enable) {
        relaySet(node, enable, 5, 3);
    }

    public void relaySet(Node node, boolean enable, int count, int step) {
        if (mMessager != null) {
            RelaySetMessage message = new RelaySetMessage(node, enable, count, step);
            message.setPostCount(mMessagePostCount);
            mMessager.relaySet(message);
        }
    }

    public void compositionDataGet(Node node) {
        compositionDataGet(node, 0);
    }

    public void setLightHSL(int color, Node node, long addr) {
        if (mMessager != null) {
            float[] hsl = new float[3];
            ColorUtils.colorToHSL(color, hsl);
            LightHSLSetMessage message = new LightHSLSetMessage(addr, node, mApp, hsl);
            mMessager.lightSetHSL(message);
        }
    }

    public void getLightHSL(Node node, long addr) {
        if (mMessager != null) {
            LightHSLGetMessage message = new LightHSLGetMessage(addr, node, mApp);
            mMessager.lightGetHSL(message);
        }
    }

    public void setLightCTL(int lightness, int temperature, int deltaUV, Node node, long addr) {
        if (mMessager != null) {
            LightCTLSetMessage message = new LightCTLSetMessage(addr, node, mApp, lightness, temperature, deltaUV);
            mMessager.lightSetCTL(message);
        }
    }

    public void getLightCTL(Node node, long addr) {
        if (mMessager != null) {
            LightCTLGetMessage message = new LightCTLGetMessage(addr, node, mApp);
            mMessager.lightGetCTL(message);
        }
    }

    public void compositionDataGet(Node node, int page) {
        if (mMessager != null) {
            CompositionDataGetMessage message = new CompositionDataGetMessage(node, page);
            message.setPostCount(mMessagePostCount);
            mMessager.compositionDataGet(message);
        }
    }

    private class MessageCB extends MessageCallback {
        @Override
        public void onAppKeyStatus(int status, long netKeyIndex, long appKeyIndex) {
        }

        @Override
        public void onCompositionDataStatus(int status, int page) {
            CompositionDataEvent event = new CompositionDataEvent(status, page);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onModelAppStatus(int status, long appKeyIndex, String nodeMac, long elementAddr, String modeId) {
            ModelAppEvent event = new ModelAppEvent(status, appKeyIndex, nodeMac, elementAddr, modeId);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onModelSubscriptionStatus(int status, long groupAddr, String nodeMac, long elementAddr, String modelId) {
            ModelSubscriptionEvent event = new ModelSubscriptionEvent(status, groupAddr, nodeMac, elementAddr, modelId);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onNodeResetStatus(long nodeAddress, String nodeMac) {
            NodeResetEvent event = new NodeResetEvent(nodeAddress, nodeMac);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onRelayStatus(int state, int count, int step) {
            RelayEvent event = new RelayEvent(state, count, step);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onLightHSLStatus(float[] hsl) {
            LightHSLEvent event = new LightHSLEvent(hsl[0], hsl[1], hsl[2]);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onLightCTLStatus(int lightness, int temperature, int deltaUV) {
            LightCTLEvent event = new LightCTLEvent(lightness, temperature, deltaUV);
            EventBus.getDefault().post(event);
        }

        @Override
        public void onFastProvNodeAddrStatus(String nodeMac, long[] addrArray) {
            FastProvAddrEvent event = new FastProvAddrEvent(nodeMac, addrArray);
            EventBus.getDefault().post(event);
        }
    }

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        mConnected = true;
                        break;
                    case BluetoothGatt.STATE_DISCONNECTED:
                        mConnected = false;
                        break;
                }
            } else {
                disconnectNode();
                mConnected = false;
            }

            GattConnectionEvent event = new GattConnectionEvent(gatt, status, newState);
            EventBus.getDefault().post(event);
        }
    }
}
