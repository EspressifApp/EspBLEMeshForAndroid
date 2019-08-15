package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.user.MeshUser;

public class AppDeleteTask implements MeshTask {
    private long mKeyIndex;

    public AppDeleteTask(long keyIndex) {
        mKeyIndex = keyIndex;
    }

    public void run() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        dbManager.deleteApp(mKeyIndex);

        MeshUser.Instance.removeApp(mKeyIndex);
    }
}
