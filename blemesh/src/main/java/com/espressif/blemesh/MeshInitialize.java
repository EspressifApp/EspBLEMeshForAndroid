package com.espressif.blemesh;

import android.content.Context;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.utils.MeshUtils;

import libs.espressif.utils.DataUtil;

public final class MeshInitialize {
    public static void init(Context context) {
        MeshObjectBox.getInstance().init(context);
        initDBData();
        MeshUser.Instance.reload();
    }

    private static void initDBData() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        // Check unassigned address
//        if (!dbManager.hasAddress(MeshConstants.ADDRESS_UNASSIGNED)) {
//            dbManager.saveAddress(MeshConstants.ADDRESS_UNASSIGNED);
//        }

        // Check default App unicast address
        if (!dbManager.hasAddress(MeshConstants.APP_ADDRESS_DEFAULT)) {
            dbManager.saveAddress(MeshConstants.APP_ADDRESS_DEFAULT);
        }

        // Check default AppKey
        if (dbManager.loadApp(MeshConstants.APP_KEY_INDEX_DEFAULT) == null) {
            byte[] appKey = MeshUtils.generateRandom();
            String appKeyStr = DataUtil.bigEndianBytesToHexString(appKey);
            dbManager.saveApp(MeshConstants.APP_KEY_INDEX_DEFAULT, appKeyStr, MeshConstants.APP_ADDRESS_DEFAULT);
        }

        // Check default NetKey
        if (dbManager.loadNetwork(MeshConstants.NET_KEY_INDEX_DEFAULT) == null) {
            byte[] netKey = MeshUtils.generateRandom();
            String netKeyStr = DataUtil.bigEndianBytesToHexString(netKey);
            dbManager.saveNetwork(MeshConstants.NET_KEY_INDEX_DEFAULT, netKeyStr, MeshConstants.NET_NAME_DEFAULT,
                    0, 0);
        }

        // Check default GroupAddress
        if (!dbManager.hasAddress(MeshConstants.ADDRESS_GROUP_MIN)) {
            dbManager.saveAddress(MeshConstants.ADDRESS_GROUP_MIN);
        }
    }
}
