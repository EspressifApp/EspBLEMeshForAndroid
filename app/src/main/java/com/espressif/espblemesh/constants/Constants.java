package com.espressif.espblemesh.constants;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.utils.MeshUtils;

public class Constants {
    public static final String KEY_SCAN_RESULT = "scan_result";
    public static final String KEY_NODE_MAC = "node_mac";
    public static final String KEY_NETWORK_INDEX = "network_index";
    public static final String KEY_GROUP_ADDRESS = "group_address";
    public static final String KEY_FAST_FROV = "fast_prov";
    public static final String KEY_OTA_PACKAGE = "ota_package";
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";

    public static final String KEY_NODE = "node";
    public static final String KEY_MODEL = "model";
    public static final String KEY_GROUPS = "groups";
    public static final String KEY_BIND_GROUPS = "bind_groups";
    public static final String KEY_OPERATION = "operation";
    public static final String KEY_DST_ADDRESS = "dst_address";
    public static final String KEY_MODEL_ID = "model_id";

    public static final long APP_ADDRESS_DEFAULT = MeshConstants.APP_ADDRESS_DEFAULT;
    public static final long APP_KEY_INDEX_DEFAULT = MeshConstants.APP_KEY_INDEX_DEFAULT;

    public static final long NET_KEY_INDEX_DEFAULT = MeshConstants.NET_KEY_INDEX_DEFAULT;
}
