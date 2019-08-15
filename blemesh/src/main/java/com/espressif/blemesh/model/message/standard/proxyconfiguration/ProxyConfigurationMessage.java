package com.espressif.blemesh.model.message.standard.proxyconfiguration;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.Message;

abstract class ProxyConfigurationMessage extends Message {
    ProxyConfigurationMessage(Node node) {
        super(MeshConstants.ADDRESS_UNASSIGNED, node);

        setCTL(1);
        setTTL(0);
    }

    @Override
    public void setCTL(int CTL) {
        super.setCTL(1);
    }

    @Override
    public void setTTL(int TTL) {
        super.setTTL(0);
    }
}
