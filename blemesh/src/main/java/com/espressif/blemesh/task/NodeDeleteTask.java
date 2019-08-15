package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.user.MeshUser;

public class NodeDeleteTask implements MeshTask {
    private String mNodeMac;

    public NodeDeleteTask(String nodeMac) {
        mNodeMac = nodeMac;
    }

    public void run() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        dbManager.deleteNode(mNodeMac);

        MeshUser user = MeshUser.Instance;
        user.removeNode(mNodeMac);
    }
}
