package com.espressif.blemesh.client.abs;

import com.espressif.blemesh.model.Node;

public interface PrivateProvisioningCallback extends PrivateMeshCallback {

    void onProvisionResult(PrivateProvisioner provisioner, int code, Node node);
}
