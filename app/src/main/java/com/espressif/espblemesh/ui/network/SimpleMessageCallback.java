package com.espressif.espblemesh.ui.network;

import com.espressif.blemesh.client.callback.MessageCallback;

public abstract class SimpleMessageCallback extends MessageCallback {
    public void onGattClosed() {
    }
}
