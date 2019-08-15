package com.espressif.blemesh.task;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.user.MeshUser;

import libs.espressif.utils.DataUtil;
import libs.espressif.utils.TextUtils;

public class NetworkAddTask implements MeshTask {
    private String mKey;
    private String mName;

    public NetworkAddTask(String key, String name) {
        mKey = key;
        mName = name;
    }

    public Network run() {
        if (TextUtils.isEmpty(mKey)) {
            throw new IllegalArgumentException("Network key can't be empty");
        }

        byte[] keyBytes = DataUtil.hexStringToBigEndianBytes(mKey);
        if (keyBytes.length !=16) {
            throw new IllegalArgumentException("Network key invalid");
        }

        long ivIndex = 0L;
        long seq = 0L;

        MeshObjectBox dbManager = MeshObjectBox.getInstance();
        long keyIndex = dbManager.saveNetwork(mKey, mName, ivIndex, seq);
        Network network = new Network(keyBytes, keyIndex, mName, ivIndex, seq);

        MeshUser user  = MeshUser.Instance;
        user.addNetwork(network);

        return network;
    }
}
