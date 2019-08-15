package com.espressif.blemesh.client;

import com.espressif.blemesh.client.callback.ProvisioningCallback;
import com.espressif.blemesh.model.Network;

public interface IMeshProvisioner {
    void release();

    void setProvisioningCallback(ProvisioningCallback provisioningCallback);

    void provisioning(String deviceName, Network network);
}
