package com.espressif.espblemesh.model;

import android.bluetooth.le.ScanResult;

public class BleScanResult {
    private ScanResult mScanResult;
    private long mScanTime;

    public BleScanResult() {
    }

    public BleScanResult(ScanResult scanResult, long scanTime) {
        mScanResult = scanResult;
        mScanTime = scanTime;
    }

    public void setScanResult(ScanResult scanResult) {
        mScanResult = scanResult;
    }

    public ScanResult getScanResult() {
        return mScanResult;
    }

    public void setScanTime(long scanTime) {
        mScanTime = scanTime;
    }

    public long getScanTime() {
        return mScanTime;
    }
}
