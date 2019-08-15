package com.espressif.espblemesh.utils;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

public class Utils {
    public static String getBLEDeviceName(ScanResult sr) {
        String name = sr.getDevice().getName();;
        if (name == null) {
            ScanRecord record = sr.getScanRecord();
            if (record != null) {
                name = record.getDeviceName();
            }
        }

        return name;
    }
}
