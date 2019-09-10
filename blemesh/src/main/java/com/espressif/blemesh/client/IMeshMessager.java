package com.espressif.blemesh.client;

import com.espressif.blemesh.client.callback.MessageCallback;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.message.Message;
import com.espressif.blemesh.model.message.custom.FastGroupBindMessage;
import com.espressif.blemesh.model.message.custom.FastGroupUnbindMessage;
import com.espressif.blemesh.model.message.custom.FastProvInfoSetMessage;
import com.espressif.blemesh.model.message.custom.FastProvNodeAddrGetMessage;
import com.espressif.blemesh.model.message.standard.AppKeyAddMessage;
import com.espressif.blemesh.model.message.standard.CompositionDataGetMessage;
import com.espressif.blemesh.model.message.standard.GenericOnOffMessage;
import com.espressif.blemesh.model.message.standard.LightCTLGetMessage;
import com.espressif.blemesh.model.message.standard.LightCTLSetMessage;
import com.espressif.blemesh.model.message.standard.LightHSLGetMessage;
import com.espressif.blemesh.model.message.standard.LightHSLSetMessage;
import com.espressif.blemesh.model.message.standard.ModelAppBindMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionAddMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionDeleteAllMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionDeleteMessage;
import com.espressif.blemesh.model.message.standard.ModelSubscriptionOverwriteMessage;
import com.espressif.blemesh.model.message.standard.RelaySetMessage;

public interface IMeshMessager {
    void release();

    void setNetwork(Network network);

    Network getNetwork();

    byte[] getDeviceUUID();

    void setMessageCallback(MessageCallback messageCallback);

    void postMessage(Message message);

    void appKeyAdd(AppKeyAddMessage message);

    void compositionDataGet(CompositionDataGetMessage message);

    void modelAppBind(ModelAppBindMessage message);

    void modelSubscriptionAdd(ModelSubscriptionAddMessage message);

    void modelSubscriptionDelete(ModelSubscriptionDeleteMessage message);

    void modelSubscriptionOverwrite(ModelSubscriptionOverwriteMessage message);

    void modelSubscriptionDeleteAll(ModelSubscriptionDeleteAllMessage message);

    void relaySet(RelaySetMessage message);

    void genericOnOff(GenericOnOffMessage message);

    void lightSetHSL(LightHSLSetMessage message);

    void lightGetHSL(LightHSLGetMessage message);

    void lightSetCTL(LightCTLSetMessage message);

    void lightGetCTL(LightCTLGetMessage message);

    void fastProvInfoSet(FastProvInfoSetMessage message);

    void fastGroupBind(FastGroupBindMessage message);

    void fastGroupUnbind(FastGroupUnbindMessage message);

    void fastProvNodeAddrGet(FastProvNodeAddrGetMessage message);
}
