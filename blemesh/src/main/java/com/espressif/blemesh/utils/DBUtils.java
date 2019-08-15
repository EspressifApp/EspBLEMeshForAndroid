package com.espressif.blemesh.utils;

import com.espressif.blemesh.db.entity.AppDB;
import com.espressif.blemesh.db.entity.ElementDB;
import com.espressif.blemesh.db.entity.GroupDB;
import com.espressif.blemesh.db.entity.ModelDB;
import com.espressif.blemesh.db.entity.NetworkDB;
import com.espressif.blemesh.db.entity.NodeDB;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Element;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;

import libs.espressif.utils.DataUtil;

public class DBUtils {
    public static Network getNetworkWithDB(NetworkDB networkDB) {
        byte[] netKey = DataUtil.hexStringToBigEndianBytes(networkDB.key);
        String netName = networkDB.name;
        long netKeyIndex = networkDB.id;
        long ivIndex = networkDB.iv_index;
        long seq = networkDB.seq;

        return new Network(netKey, netKeyIndex, netName, ivIndex, seq);
    }

    public static App getAppWithDB(AppDB appDB) {
        byte[] appKey = DataUtil.hexStringToBigEndianBytes(appDB.key);
        App app = new App(appKey, appDB.id);
        app.setUnicastAddr(appDB.unicast_addr);
        return app;
    }

    public static Group getGroupWithDB(GroupDB groupDB) {
        Group group = new Group();
        group.setAddress(groupDB.id);
        group.setName(groupDB.name);
        group.setNetKeyIndex(groupDB.netkey_index);
        return group;
    }

    public static Node getNodeWithDB(NodeDB nodeDB) {
        Node node = new Node();
        node.setMac(nodeDB.mac);
        node.setUUID(nodeDB.uuid);
        node.setName(nodeDB.name);
        node.setDeviceKey(DataUtil.hexStringToBigEndianBytes(nodeDB.key));
        node.setUnicastAddress(nodeDB.unicast_addr);
        node.setElementCount(nodeDB.element_count);
        node.setNetKeyIndex(nodeDB.netkey_index);

        node.setCid(nodeDB.cid);
        node.setPid(nodeDB.pid);
        node.setVid(nodeDB.vid);
        node.setCrpl(nodeDB.crpl);
        node.setFeatures(nodeDB.features);

        return node;
    }

    public static Element getElementWithDB(ElementDB elementDB) {
        Element element = new Element();
        element.setUnicastAddress(elementDB.id);
        element.setLoc(elementDB.loc);
        element.setNodeMac(elementDB.node_mac);

        return element;
    }

    public static Model getModelWithDB(ModelDB modelDB) {
        Model model = new Model();
        model.setId(modelDB.model_id);
        model.setElementAddress(modelDB.element_address);
        model.setNodeMac(modelDB.node_mac);
        model.setAppKeyIndex(modelDB.app_key_index);

        return model;
    }
}
