package com.espressif.espblemesh.ota;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.espressif.blemesh.client.IMeshMessager;
import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.custom.OtaNBVNMessage;
import com.espressif.blemesh.model.message.custom.OtaStartMessage;
import com.espressif.blemesh.model.message.standard.proxyconfiguration.AddAddressesToFilterMessage;
import com.espressif.blemesh.utils.MeshAlgorithmUtils;
import com.espressif.blemesh.utils.MeshUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import libs.espressif.log.EspLog;
import libs.espressif.net.NetUtil;
import libs.espressif.utils.DataUtil;

public class OTAClient {
    private static final String TCP_HOST = "192.168.4.1";
    private static final int TCP_PORT = 80;

    private EspLog mLog = new EspLog(getClass());

    private Activity mActivity;
    private WifiManager mWifiManager;
    private WifiReceiver mWifiReceiver;
    private volatile boolean mWifiReceiverRgst;

    private IMeshMessager mGattMessager;
    private Node mNode;

    private File mBinFile;
    private byte[] mManufacturerID;
    private byte[] mBinID;
    private byte[] mVersion;

    private byte[] mSoftApSSID;
    private byte[] mSoftApPassword;
    private final Object mWifiScanLock = new Object();
    private final Object mWifiConnLock = new Object();

    private boolean mRecvNOUN = false;
    private boolean mRecvStartResp = false;

    private Thread mTaskThread;
    private boolean mClosed;
    private Socket mSocket;

    private SoftAPCallback mSoftAPCallback;

    public OTAClient(Activity activity, Node node, IMeshMessager messager,
                     File bin, byte[] softApSSID, byte[] softApPassword) {
        mActivity = activity;

        mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new WifiReceiver();
        mWifiReceiverRgst = false;

        mGattMessager = messager;
        mNode = node;

        mBinFile = bin;
        String name = mBinFile.getName();
        String[] nameSplits = name.split("_");
        int manufacturerId = Integer.parseInt(nameSplits[0], 16);
        mManufacturerID = new byte[]{(byte) (manufacturerId & 0xff), (byte) ((manufacturerId >> 8) & 0xff)};
        int binId = Integer.parseInt(nameSplits[1]);
        mBinID = new byte[]{(byte) (binId & 0xff), (byte) ((binId >> 8) & 0xff)};
        String[] verStrs = nameSplits[2].split("\\.");
        String bigVerStr = verStrs[0];
        byte bigVer = (byte) Integer.parseInt(bigVerStr);
        int littleVer0 = Integer.parseInt(verStrs[1]) & 0xff;
        int littleVer1 = Integer.parseInt(verStrs[2]) & 0xff;
        byte littleVer = (byte) (littleVer0 << 4 | littleVer1);
        mVersion = new byte[]{bigVer, littleVer};

        mSoftApSSID = softApSSID;
        mSoftApPassword = softApPassword;

        mClosed = false;

        mLog.i(String.format("Manufacturer ID data = %s | Bin ID data = %s | Version data = %s",
                DataUtil.bigEndianBytesToHexString(mManufacturerID),
                DataUtil.bigEndianBytesToHexString(mBinID),
                DataUtil.bigEndianBytesToHexString(mVersion)));
    }

    public synchronized void close() {
        mLog.d("Close OTA Client");
        mClosed = true;

        if (mTaskThread != null) {
            mTaskThread.interrupt();
            mTaskThread = null;
        }

        unregisterBroadcast();
        mActivity = null;

        mGattMessager = null;

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;

        }
    }

    public void setSoftAPCallback(SoftAPCallback softAPCallback) {
        mSoftAPCallback = softAPCallback;
    }

    private synchronized void registerBroadcast() {
        if (mActivity != null && !mWifiReceiverRgst) {
            final LinkedBlockingQueue<Object> lock = new LinkedBlockingQueue<>();
            mActivity.runOnUiThread(() -> {
                IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                mActivity.registerReceiver(mWifiReceiver, filter);
                mWifiReceiverRgst = true;
                lock.add(new Object());
            });
            try {
                lock.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void unregisterBroadcast() {
        if (mActivity != null && mWifiReceiverRgst) {
            final LinkedBlockingQueue<Object> lock = new LinkedBlockingQueue<>();
            mActivity.runOnUiThread(() -> {
                mActivity.unregisterReceiver(mWifiReceiver);
                mWifiReceiverRgst = false;
                lock.add(new Object());
            });
            try {
                lock.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isTaskCanceled() {
        return Thread.currentThread().isInterrupted() || mClosed;
    }

    public void ota() {
        mLog.d("~~~~~OTA start~~~~~");
        start();

        close();
        mLog.d("~~~~~OTA over~~~~~");
    }

    public void notifyWifiConnected() {
        mLog.d("notifyWifiConnected");
        synchronized (mWifiConnLock) {
            mWifiConnLock.notify();
        }
    }

    private void start() {
        if (mClosed) {
            throw new IllegalStateException("OTA Client is closed");
        }

        mTaskThread = Thread.currentThread();

        final int pkgLength = 1024;
        int binLength = (int) mBinFile.length();
        final int pkgBinLength = 1024 - 4 - 2 - 2 - 16;
        final int pkgCount = binLength / pkgBinLength + (binLength % pkgBinLength == 0 ? 0 : 1);

        // Read bin data
        mLog.d("Read bin data. Package count = " + pkgCount);
        List<byte[]> binDataList = new ArrayList<>(pkgCount);
        try {
            FileInputStream fis = new FileInputStream(mBinFile);
            for (int i = 0; i < pkgCount; i++) {
                byte[] binData = new byte[pkgBinLength];
                int readLen = fis.read(binData);
                if (readLen < pkgBinLength) {
                    binData = Arrays.copyOf(binData, readLen);
                }

                binDataList.add(binData);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Send NBVN
        mLog.d("Send NBVN");
        do {
            if (isTaskCanceled()) {
                return;
            }

            List<byte[]> whiteList = new ArrayList<>(1);
            whiteList.add(MeshUtils.addressLongToBigEndianBytes(MeshConstants.ADDRESS_OTA_GROUP));
            AddAddressesToFilterMessage aatf = new AddAddressesToFilterMessage(mNode, whiteList);
            // TODO add addAddressesToFilter in IMeshMessager
//            mGattMessager.addAddressesToFilter(aatf);

            // TODO set msg data
            OtaNBVNMessage otaNBVN = new OtaNBVNMessage(mNode, mBinID);

            otaNBVN.setManufacturerID(mManufacturerID);
            otaNBVN.setVersion(mVersion);
            // TODO add otaNewBinVersionNotification in IMeshMessager
//            mGattMessager.otaNewBinVersionNotification(otaNBVN);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            }
        } while (!mRecvNOUN);

        // Send OTA start
        mLog.d("Send OTA start");
        do {
            if (isTaskCanceled()) {
                return;
            }

            // TODO set msg data
            OtaStartMessage otaStart = new OtaStartMessage(mNode, mBinID);
            otaStart.setManufacturerID(mManufacturerID);
            otaStart.setSoftApSSID(mSoftApSSID);
            otaStart.setSoftApPassword(mSoftApPassword);
            // TODO add otaStart in IMeshMessager
//            mGattMessager.otaStart(otaStart);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            }
        } while (!mRecvStartResp);

        // Connect SoftAP
        mLog.d("Start connect SoftAP");
        boolean connectSoftAp = false;
        String ssidStr = new String(mSoftApSSID);
        String pwdStr = new String(mSoftApPassword);
        if (mWifiManager.isWifiEnabled()) {
            synchronized (mWifiScanLock) {
                registerBroadcast();
                mLog.d("Scan Wifi list");
                boolean scan = mWifiManager.startScan();
                if (scan) {
                    try {
                        mWifiScanLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            List<ScanResult> scanResults = mWifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                String scanSSID = scanResult.SSID;
                if (ssidStr.equals(scanSSID)) {
                    mLog.d("Scanned SoftAP");
                    break;
                }
            }

            mLog.d("Try connecting SoftAP");
            WifiConfiguration wifiConfiguration = NetUtil.newWifiConfigration(NetUtil.WIFI_SECURITY_WPA,
                    ssidStr, pwdStr, false);
            int netId = mWifiManager.addNetwork(wifiConfiguration);
            mWifiManager.enableNetwork(netId, true);
            for (int i = 0; i < 30; i++) {
                if (isTaskCanceled()) {
                    return;
                }

                String connSSID = NetUtil.getCurrentConnectSSID(mActivity.getApplicationContext());
                if (ssidStr.equals(connSSID)) {
                    mLog.d("Check connected " + ssidStr);
                    connectSoftAp = true;
                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    return;
                }
            }

        }

        while (!connectSoftAp) {
            if (isTaskCanceled()) {
                return;
            }

            synchronized (mWifiConnLock) {
                mLog.d("Wait user connect SoftAP");
                if (mSoftAPCallback != null) {
                    mActivity.runOnUiThread(() -> mSoftAPCallback.onConnectedResult(false));
                }

                try {
                    mWifiConnLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            String connSSID = NetUtil.getCurrentConnectSSID(mActivity.getApplicationContext());
            if (ssidStr.equals(connSSID)) {
                connectSoftAp = true;
                if (mSoftAPCallback != null) {
                    mLog.d("User connected SoftAP");
                    mActivity.runOnUiThread(() -> mSoftAPCallback.onConnectedResult(true));
                }
                break;
            }
        }

        // Post bin data
        mLog.d("Start post bin");
        for (int t = 0; t < 5; t++) {
            mLog.d("Try post times " + t);
            if (isTaskCanceled()) {
                return;
            }

            if (mSocket == null) {
                mLog.d("Socket connect");
                for (int i = 0; i < 3; i++) {
                    if (isTaskCanceled()) {
                        return;
                    }

                    try {
                        mSocket = new Socket(TCP_HOST, TCP_PORT);
                        mSocket.setSendBufferSize(pkgLength);
                        mSocket.setSoTimeout(10000);
                        mLog.d("Socket connect suc");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mSocket == null) {
                continue;
            }

            mLog.d("Posting bin data");
            try {
                boolean suc = postBin(binDataList);
                if (suc) {
                    mLog.d("Post bin data complete");
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();

                try {
                    mSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                mSocket = null;
            }
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;
        }
    }

    public void onReceiveNeedOTAUpdateNotification() {
        mRecvNOUN = true;
    }

    public void onReceiveOTAStartResponse() {
        mRecvStartResp = true;
    }

    private boolean postBin(List<byte[]> binDataList) throws IOException {
        // 0x02e5eb00 maxSeq CurrentSeq Data Hash
        final int pkgCount = binDataList.size();
        byte[] maxSeq = {(byte) ((pkgCount - 1) & 0xff), (byte) (((pkgCount - 1) >> 8) & 0xff)};

        final int checkDataLen = pkgCount / 8 + (pkgCount % 8 == 0 ? 0 : 1);
        final int[] checkData = new int[checkDataLen];

        final byte[] hashKey = new byte[16];
        for (int t = 0; t < 10; t++) {
            // Try 10 times

            // Post bin data
            for (int i = 0; i < pkgCount; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }

                int index = i % 8;
                int offset = i % 8;
                if (((checkData[index] >> offset) & 1) == 1) {
                    continue;
                }

                byte[] binData = binDataList.get(i);
                byte[] hash = MeshAlgorithmUtils.AES_CMAC(hashKey, binData);
                OutputStream os = mSocket.getOutputStream();
                os.write(mManufacturerID);
                os.write(maxSeq);
                os.write(i & 0xff);
                os.write((i >> 8) & 0xff);
                os.write(binData);
                os.write(hash);
                os.flush();
            }

            // Receive response
            InputStream is = mSocket.getInputStream();
            if ((is.read() & 0xff) != (mManufacturerID[0] & 0xff)) {
                return false;
            }
            if ((is.read() & 0xff) != (mManufacturerID[1] & 0xff)) {
                return false;
            }
            if ((is.read() & 0xff) != 0xEB) {
                return false;
            }
            if ((is.read() & 0xff) != 0x03) {
                return false;
            }

            for (int i = 0; i < checkDataLen; i++) {
                checkData[i] = is.read() & 0xff;
            }
            boolean suc = true;
            for (int i = 0; i < pkgCount; i++) {
                int index = i % 8;
                int offset = i % 8;

                if (((checkData[index] >> offset) & 1) == 0) {
                    suc = false;
                    break;
                }
            }

            if (suc) {
                return true;
            }
        }

        return false;
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    synchronized (mWifiScanLock) {
                        mWifiScanLock.notify();
                    }
                    break;
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    break;
            }
        }
    }

    public interface SoftAPCallback {
        void onConnectedResult(boolean connected);
    }
}
