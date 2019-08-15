package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.db.entity.GroupDB;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.utils.DBUtils;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.blemesh.user.MeshUser;

public class GroupAddTask implements MeshTask {
    private String mGroupName;
    private Network mNetwork;

    public GroupAddTask(String groupName, Network network) {
        mGroupName = groupName;
        mNetwork = network;
    }

    public Group run() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        long groupAddr = MeshUtils.generateGroupAddress();
        dbManager.saveGroup(groupAddr, mGroupName, mNetwork.getKeyIndex());
        GroupDB groupDB = new GroupDB();
        groupDB.id = groupAddr;
        groupDB.name = mGroupName;
        groupDB.netkey_index = mNetwork.getKeyIndex();
        Group group = DBUtils.getGroupWithDB(groupDB);
        mNetwork.addGroup(groupAddr);
        MeshUser.Instance.addGroup(group);
        return group;
    }
}
