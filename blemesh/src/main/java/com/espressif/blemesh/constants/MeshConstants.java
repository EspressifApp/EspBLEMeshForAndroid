package com.espressif.blemesh.constants;

import java.util.UUID;

public class MeshConstants {
    public static final UUID UUID_DEVICE_SERVICE = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DEVICE_CHAR_WRITE = UUID.fromString("00002adb-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DEVICE_CHAR_NOTIFICATION = UUID.fromString("00002adc-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_NODE_SERVICE = UUID.fromString("00001828-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_NODE_CHAR_WRITE = UUID.fromString("00002add-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_NODE_CHAR_NOTIFICATION = UUID.fromString("00002ade-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_DESC_NOTIFICATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int MTU_LENGTH_DEFAULT = 69;
    public static final int MTU_LENGTH_MIN = 23;

    public static final int PROXY_SAR_COMPLETE = 0;
    public static final int PROXY_SAR_FIRST = 1;
    public static final int PROXY_SAR_CONTINUATION = 2;
    public static final int PROXY_SAR_LAST = 3;

    public static final int ADV_PROVISIONING_PDU_TYPE = 0x29;
    public static final int ADV_NETWORK_PDU_TYPE = 0x2a;
    public static final int ADV_BEACON_TYPE = 0x2b;

    public static final int GPCF_TRANSACTION_START = 0;
    public static final int GPCF_TRANSACTION_ACK = 1;
    public static final int GPCF_TRANSACTION_CONTINUATION = 2;
    public static final int GPCF_PROVISIONING_BEARER_CONTROL = 3;

    public static final int PROXY_TYPE_NETWORK_PDU = 0x00;
    public static final int PROXY_TYPE_MESH_BEACON = 0x01;
    public static final int PROXY_TYPE_PROXY_CONGURATION = 0x02;
    public static final int PROXY_TYPE_PROVISIONING_PDU = 0x03;

    public static final int PROVISIONING_TYPE_INVITE = 0x00;
    public static final int PROVISIONING_TYPE_CAPABILITIES = 0x01;
    public static final int PROVISIONING_TYPE_START = 0x02;
    public static final int PROVISIONING_TYPE_PUBLIC_KEY = 0x03;
    public static final int PROVISIONING_TYPE_INPUT_COMPLETE = 0x04;
    public static final int PROVISIONING_TYPE_CONFIRMATION = 0x05;
    public static final int PROVISIONING_TYPE_RANDOM = 0x06;
    public static final int PROVISIONING_TYPE_DATA = 0x07;
    public static final int PROVISIONING_TYPE_COMPLETE = 0x08;
    public static final int PROVISIONING_TYPE_FAILED = 0x09;

    public static final byte[] BYTES_PRCK = "prck".getBytes();
    public static final byte[] BYTES_PRSK = "prsk".getBytes();
    public static final byte[] BYTES_PRSN = "prsn".getBytes();
    public static final byte[] BYTES_PRDK = "prdk".getBytes();
    public static final byte[] BYTES_SMK2 = "smk2".getBytes();
    public static final byte[] BYTES_SMK3 = "smk3".getBytes();
    public static final byte[] BYTES_SMK4 = "smk4".getBytes();
    public static final byte[] BYTES_ID64 = "id64".getBytes();
    public static final byte[] BYTES_ID6 = "id6".getBytes();

    public static final byte[] AUTH_VALUE_ZERO = new byte[]{
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
    };

    public static final long ADDRESS_UNASSIGNED = 0x0000;
    public static final long ADDRESS_UNICAST_MIN = 0x0001;
    public static final long ADDRESS_UNICAST_MAX = 0x7fff;
    public static final long ADDRESS_VIRTUAL_MIN = 0x8000;
    public static final long ADDRESS_VIRTUAL_MAX = 0xbfff;
    public static final long ADDRESS_GROUP_MIN = 0xc000;
    public static final long ADDRESS_GROUP_MAX = 0xffff;

    public static final long ADDRESS_OTA_GROUP = 0xf000;

    public static final long APP_ADDRESS_DEFAULT = 0x0001;
    public static final long APP_KEY_INDEX_DEFAULT = 0x0001;

    public static final long NET_KEY_INDEX_DEFAULT = 0x0001;
    public static final String NET_NAME_DEFAULT = "Default Network";

    public static final String MODEL_ID_ONOFF = "1000";
    public static final String MODEL_ID_HSL = "1307";
}
