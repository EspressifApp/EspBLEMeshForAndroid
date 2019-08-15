package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.db.entity.AppDB;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.blemesh.user.MeshUser;

import libs.espressif.utils.DataUtil;

public class AppAddTask implements MeshTask {
    private String mAppKey;

    public AppAddTask(String appKey) {
        mAppKey = appKey;
    }

    public App run() {
        byte[] keyBytes = DataUtil.hexStringToBigEndianBytes(mAppKey);

        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        AppDB appDB = dbManager.loadAppForAppKey(mAppKey);
        if (appDB != null) {
            return null;
        }

        long addr = MeshUtils.generateUnicastAddress();
        long keyIndex = dbManager.saveApp(mAppKey, addr);
        App app = new App(keyBytes, keyIndex);
        app.setUnicastAddr(addr);

        MeshUser user = MeshUser.Instance;
        user.addApp(app);

        return app;
    }
}
