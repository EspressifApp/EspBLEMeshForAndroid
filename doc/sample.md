# Sample
## Initialize
```java
MeshInitialize.init(context);
```

## Load data from DB
```java
MeshUser.Instance.reload();    
```

## Provisioning
```java
MeshGattCallback meshGattCB = new MeshGattCallback() {
    @Override
    public void onDiscoverDeviceServiceResult(int code, MeshGattProvisioner provisioner) {
        if (code == MeshGattCallback.CODE_SUCCESS) {
            // Connect device and discover service complete, provosioning here
            String nodeName = null; // Set custom node name
            Network network = null; // Set specific Network
            provisioner.setProvisioningCallback(new ProvisioningCallback() {
                @Override
                public void onProvisioningFailed(int code) {
                    // Provisioning failed
                }

                @Override
                public void onProvisioningSuccess(int code, Node node) {
                    // Provisioning complete
                }
            });
            provisioner.provisioning(nodeName, network); // Start provisioning
        }
    }
}
MeshGattClient client = new MeshGattClient(device);
client.setGattCallback(new BluetoothGattCallback());
client.setMeshCallback(meshGattCB);
client.connect(context);
```

## Message post and get
```java
MeshGattCallback meshGattCB = new MeshGattCallback() {
    @Override
    public void onDiscoverNodeServiceResult(int code, IMeshMessager messager) {
        if (code == MeshGattCallback.CODE_SUCCESS) {
            // Connect and discover node service complete
            messager.setNetwork(network); // Set Network
            messager.setMessageCallback(new MessageCallback()); // See callback function in class MessageCallback
            // Can send message now
            messager.postMessage(message); // See Message implements in package com.espressif.blemesh.model.message
        }
    }
}
MeshGattClient client = new MeshGattClient(device);
client.setGattCallback(new BluetoothGattCallback());
client.setMeshCallback(meshGattCB);
client.setAppAddr(AppAddress);
client.connect(context);
```