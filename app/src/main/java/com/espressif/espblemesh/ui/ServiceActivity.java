package com.espressif.espblemesh.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.espressif.espblemesh.app.BaseActivity;

public abstract class ServiceActivity extends BaseActivity {
    private MainService mService;
    private ServiceConn mServiceConn;

    private boolean mPaused;

    protected boolean willAutoScan() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(this, MainService.class);
        mServiceConn = new ServiceConn();
        bindService(serviceIntent, mServiceConn, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPaused = false;

        if (mService != null && willAutoScan()) {
            mService.startScanBle();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPaused = true;

        if (mService != null) {
            mService.stopScanBle();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConn);
    }

    protected void onServiceConnected(ComponentName name, IBinder service) {
    }

    protected void onServiceDisconnected(ComponentName name) {
    }

    public MainService getService() {
        return mService;
    }

    protected boolean isPaused() {
        return mPaused;
    }

    private class ServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainService.MainServiceBinder binder = (MainService.MainServiceBinder) service;
            mService = binder.getService();

            if (!isPaused() && willAutoScan()) {
                mService.startScanBle();
            }

            ServiceActivity.this.onServiceConnected(name, service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ServiceActivity.this.onServiceDisconnected(name);

            mService = null;
        }
    }
}
