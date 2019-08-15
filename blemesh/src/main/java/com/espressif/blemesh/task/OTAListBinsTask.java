package com.espressif.blemesh.task;

import android.bluetooth.BluetoothDevice;

import com.espressif.blemesh.client.MeshGattClient;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class OTAListBinsTask implements MeshTask {
    private String mDirPath;

    public OTAListBinsTask(String dirPath) {
        mDirPath = dirPath;
    }

    public List<File> run() {
        File dir = new File(mDirPath);
        System.out.println(mDirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return Collections.emptyList();
            }
        }

        BluetoothDevice device = null;
        MeshGattClient gattClient = new MeshGattClient(device);

        File[] binArray = dir.listFiles((dir1, name) -> name.toLowerCase(Locale.ENGLISH).endsWith("bin"));
        return Arrays.asList(binArray);
    }
}
