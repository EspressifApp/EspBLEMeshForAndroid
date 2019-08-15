package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.user.MeshUser;

public class NetworkDeleteTask implements MeshTask {
    private long mNetKeyIndex;

    public NetworkDeleteTask(long netKeyIndex) {
        mNetKeyIndex = netKeyIndex;
    }

    public void run() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        dbManager.deleteNetwork(mNetKeyIndex);

        MeshUser user = MeshUser.Instance;
        user.removeNetwork(mNetKeyIndex);
    }
}
