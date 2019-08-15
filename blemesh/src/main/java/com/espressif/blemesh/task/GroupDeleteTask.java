package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.user.MeshUser;

public class GroupDeleteTask implements MeshTask {
    private long mGroupAddr;

    public GroupDeleteTask(long groupAddr) {
        mGroupAddr = groupAddr;
    }

    public void run() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        dbManager.deleteGroup(mGroupAddr);

        MeshUser user = MeshUser.Instance;
        user.removeGroup(mGroupAddr);
    }
}
