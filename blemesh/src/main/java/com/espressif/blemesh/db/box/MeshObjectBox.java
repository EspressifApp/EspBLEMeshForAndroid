package com.espressif.blemesh.db.box;

import android.content.Context;

import com.espressif.blemesh.db.entity.AddressDB;
import com.espressif.blemesh.db.entity.AppDB;
import com.espressif.blemesh.db.entity.AppDB_;
import com.espressif.blemesh.db.entity.AppNodeDB;
import com.espressif.blemesh.db.entity.AppNodeDB_;
import com.espressif.blemesh.db.entity.ElementDB;
import com.espressif.blemesh.db.entity.ElementDB_;
import com.espressif.blemesh.db.entity.FastGroupDB;
import com.espressif.blemesh.db.entity.GroupDB;
import com.espressif.blemesh.db.entity.GroupDB_;
import com.espressif.blemesh.db.entity.GroupNodeDB;
import com.espressif.blemesh.db.entity.GroupNodeDB_;
import com.espressif.blemesh.db.entity.ModelDB;
import com.espressif.blemesh.db.entity.ModelDB_;
import com.espressif.blemesh.db.entity.MyObjectBox;
import com.espressif.blemesh.db.entity.NetworkDB;
import com.espressif.blemesh.db.entity.NodeDB;
import com.espressif.blemesh.db.entity.NodeDB_;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class MeshObjectBox {
    private static MeshObjectBox sInstance;

    private BoxStore mBoxStore;

    public static MeshObjectBox getInstance() {
        if (sInstance == null) {
            synchronized (MeshObjectBox.class) {
                if (sInstance == null) {
                    sInstance = new MeshObjectBox();
                }
            }
        }

        return sInstance;
    }

    private MeshObjectBox() {
    }

    public synchronized void init(Context context) {
        if (mBoxStore != null) {
            throw new IllegalStateException("MeshObjectBox has initialized");
        }

        mBoxStore = MyObjectBox.builder().androidContext(context).build();
    }

    public synchronized void close() {
        if (mBoxStore != null) {
            mBoxStore.close();
            mBoxStore = null;
        }
    }

    public List<NodeDB> loadNodeListForNetwork(long netKeyIndex) {
        return mBoxStore.boxFor(NodeDB.class)
                .query()
                .equal(NodeDB_.netkey_index, netKeyIndex)
                .build()
                .find();
    }

    public List<NodeDB> loadAllNodes() {
        return mBoxStore.boxFor(NodeDB.class).getAll();
    }

    public void saveNode(String mac, String uuid, String key, String name, long unicastAddr, int elementCount,
                         long netKeyIndex) {
        Box<NodeDB> box = mBoxStore.boxFor(NodeDB.class);
        NodeDB entity = box.query().equal(NodeDB_.mac, mac).build().findUnique();
        if (entity == null) {
            entity = new NodeDB();
        }
        entity.mac = mac;
        entity.uuid = uuid;
        entity.key = key;
        entity.name = name;
        entity.unicast_addr = unicastAddr;
        entity.element_count = elementCount;
        entity.netkey_index = netKeyIndex;

        box.put(entity);
    }

    public void updateNode(String mac, long cid, long pid, long vid, long crpl, long features) {
        Box<NodeDB> box = mBoxStore.boxFor(NodeDB.class);
        NodeDB entity = box.query().equal(NodeDB_.mac, mac).build().findUnique();
        if (entity != null) {
            entity.cid = cid;
            entity.pid = pid;
            entity.vid = vid;
            entity.crpl = crpl;
            entity.features = features;

            box.put(entity);
        }
    }

    public void deleteNode(String nodeMac) {
        // Delete AppKey bind by node
        deleteAppNodeForNodeMac(nodeMac);

        // Delete nodes in all groups
        Box<GroupNodeDB> groupNodeBox = mBoxStore.boxFor(GroupNodeDB.class);
        List<GroupNodeDB> groupNodeDBS = groupNodeBox
                .query()
                .equal(GroupNodeDB_.node_mac, nodeMac)
                .build()
                .find();
        groupNodeBox.remove(groupNodeDBS);

        Box<NodeDB> nodeBox = mBoxStore.boxFor(NodeDB.class);
        NodeDB nodeDB = nodeBox.query().equal(NodeDB_.mac, nodeMac).build().findUnique();
        if (nodeDB != null) {
            // Delete addresses
            Box<AddressDB> addressBox = mBoxStore.boxFor(AddressDB.class);
            for (int i = 0; i < nodeDB.element_count; i++) {
                long addr = nodeDB.unicast_addr + i;
                addressBox.remove(addr);
            }
            // Delete node
            nodeBox.remove(nodeDB);
        }

        deleteModelsForNode(nodeMac);

        deleteElementsForNode(nodeMac);
    }

    public ElementDB loadElement(long unicastAddr) {
        return mBoxStore.boxFor(ElementDB.class).get(unicastAddr);
    }

    public List<ElementDB> loadElementsForNode(String nodeMac) {
        return mBoxStore.boxFor(ElementDB.class)
                .query()
                .equal(ElementDB_.node_mac, nodeMac)
                .build()
                .find();
    }

    public List<ElementDB> loadAllElements() {
        return mBoxStore.boxFor(ElementDB.class).getAll();
    }

    public void saveElement(long unicastAddr, String nodeMac, long loc) {
        ElementDB elementDB = new ElementDB();
        elementDB.id = unicastAddr;
        elementDB.node_mac = nodeMac;
        elementDB.loc = loc;
        mBoxStore.boxFor(ElementDB.class).put(elementDB);
    }

    public void deleteElement(long elementAddress) {
        mBoxStore.boxFor(ElementDB.class).remove(elementAddress);
    }

    public void deleteElementsForNode(String nodeMac) {
        Box<ElementDB> box = mBoxStore.boxFor(ElementDB.class);
        List<ElementDB> entities = box.query()
                .equal(ElementDB_.node_mac, nodeMac)
                .build()
                .find();
        box.remove(entities);
    }

    public void saveModel(String modelID, long elementAddr, String nodeMac) {
        ModelDB modelDB = loadModel(modelID, elementAddr);
        if (modelDB == null) {
            modelDB = new ModelDB();
            modelDB.model_id = modelID;
            modelDB.element_address = elementAddr;
            modelDB.node_mac = nodeMac;
            modelDB.app_key_index = -1;
        }
        mBoxStore.boxFor(ModelDB.class).put(modelDB);
    }

    public void updaeModelAppKeyIndex(String modelID, long elementAddr, long appKeyIndex) {
        ModelDB modelDB = loadModel(modelID, elementAddr);
        if (modelDB != null) {
            modelDB.app_key_index = appKeyIndex;
            mBoxStore.boxFor(ModelDB.class).put(modelDB);
        }
    }

    public ModelDB loadModel(String modelID, long elementAddr) {
        return mBoxStore.boxFor(ModelDB.class)
                .query()
                .equal(ModelDB_.model_id, modelID)
                .and()
                .equal(ModelDB_.element_address, elementAddr)
                .build()
                .findUnique();
    }

    public List<ModelDB> loadModelsForElement(long elementAddr) {
        return mBoxStore.boxFor(ModelDB.class)
                .query()
                .equal(ModelDB_.element_address, elementAddr)
                .build()
                .find();
    }

    public void deleteModelsForNode(String nodeMac) {
        Box<ModelDB> box = mBoxStore.boxFor(ModelDB.class);
        List<ModelDB> entities = box.query()
                .equal(ModelDB_.node_mac, nodeMac)
                .build()
                .find();
        box.remove(entities);
    }

    public void saveAddress(long address) {
        AddressDB addressDB = new AddressDB();
        addressDB.id = address;
        mBoxStore.boxFor(AddressDB.class).put(addressDB);
    }

    public void deleteAddress(long address) {
        mBoxStore.boxFor(AddressDB.class).remove(address);
    }

    public boolean hasAddress(long address) {
        return mBoxStore.boxFor(AddressDB.class).get(address) != null;
    }

    public long saveNetwork(String netKey, String netName, long ivIndex, long seq) {
        return saveNetwork(0, netKey, netName, ivIndex, seq);
    }

    public long saveNetwork(long keyIndex, String netKey, String netName, long ivIndex, long seq) {
        NetworkDB entity = new NetworkDB();
        entity.id = keyIndex;
        entity.key = netKey;
        entity.name = netName;
        entity.iv_index = ivIndex;
        entity.seq = seq;

        return mBoxStore.boxFor(NetworkDB.class).put(entity);
    }

    public void updateNetworkSeq(long keyIndex, long seq, long ivIndex) {
        Box<NetworkDB> box = mBoxStore.boxFor(NetworkDB.class);
        NetworkDB networkDB = box.get(keyIndex);
        if (networkDB != null) {
            networkDB.seq = seq;
            networkDB.iv_index = ivIndex;
            box.put(networkDB);
        }
    }

    public NetworkDB loadNetwork(long keyIndex) {
        return mBoxStore.boxFor(NetworkDB.class).get(keyIndex);
    }

    public List<NetworkDB> loadAllNetworks() {
        return mBoxStore.boxFor(NetworkDB.class).getAll();
    }

    public void deleteNetwork(long keyIndex) {
        deleteNetworkGroups(keyIndex);
        deleteNetworkNodes(keyIndex);

        mBoxStore.boxFor(NetworkDB.class).remove(keyIndex);
    }

    public void deleteNetworkGroups(long netKeyIndex) {
        Box<GroupDB> groupBox = mBoxStore.boxFor(GroupDB.class);
        List<GroupDB> groupDBS = groupBox.query()
                .equal(GroupDB_.netkey_index, netKeyIndex)
                .build()
                .find();
        for (GroupDB groupDB : groupDBS) {
            deleteGroupNodes(groupDB.id);
        }
        groupBox.remove(groupDBS);
    }

    public void deleteNetworkNodes(long netKeyIndex) {
        Box<NodeDB> box = mBoxStore.boxFor(NodeDB.class);
        List<NodeDB> nodeDBS = box.query()
                .equal(NodeDB_.netkey_index, netKeyIndex)
                .build()
                .find();
        for (NodeDB nodeDB : nodeDBS) {
            deleteNode(nodeDB.mac);
        }
    }

    public AppDB loadAppForAppKey(String key) {
        return mBoxStore.boxFor(AppDB.class)
                .query()
                .equal(AppDB_.key, key)
                .build()
                .findUnique();
    }

    public long saveApp(String key, long unicastAddr) {
        return saveApp(0, key, unicastAddr);
    }

    public long saveApp(long keyIndex, String key, long unicastAddr) {
        AppDB entity = new AppDB();
        entity.id = keyIndex;
        entity.key = key;
        entity.unicast_addr = unicastAddr;
        return mBoxStore.boxFor(AppDB.class).put(entity);
    }

    public AppDB loadApp(long keyIndex) {
        return mBoxStore.boxFor(AppDB.class).get(keyIndex);
    }

    public List<AppDB> loadAllApps() {
        return mBoxStore.boxFor(AppDB.class).getAll();
    }

    public void deleteApp(long keyIndex) {
        deleteAppNodeForAppKeyIndex(keyIndex);
        mBoxStore.boxFor(AppDB.class).remove(keyIndex);
    }

    public long saveAppNode(long appKeyIndex, String nodeMac) {
        Box<AppNodeDB> box = mBoxStore.boxFor(AppNodeDB.class);
        AppNodeDB appNodeDB = box
                .query()
                .equal(AppNodeDB_.app_key_index, appKeyIndex)
                .and()
                .equal(AppNodeDB_.node_mac, nodeMac)
                .build()
                .findUnique();
        if (appNodeDB != null) {
            return appNodeDB.id;
        } else {
            appNodeDB = new AppNodeDB();
            appNodeDB.app_key_index = appKeyIndex;
            appNodeDB.node_mac = nodeMac;
            return box.put(appNodeDB);
        }
    }

    public List<AppNodeDB> loadAppNodeForNodeMac(String nodeMac) {
        return mBoxStore.boxFor(AppNodeDB.class)
                .query()
                .equal(AppNodeDB_.node_mac, nodeMac)
                .build()
                .find();
    }

    public void deleteAppNodeForNodeMac(String nodeMac) {
        Box<AppNodeDB> box = mBoxStore.boxFor(AppNodeDB.class);
        List<AppNodeDB> entities = box
                .query()
                .equal(AppNodeDB_.node_mac, nodeMac)
                .build()
                .find();
        box.remove(entities);
    }

    public void deleteAppNodeForAppKeyIndex(long appKeyIndex) {
        Box<AppNodeDB> box = mBoxStore.boxFor(AppNodeDB.class);
        List<AppNodeDB> entities = box
                .query()
                .equal(AppNodeDB_.app_key_index, appKeyIndex)
                .build()
                .find();
        box.remove(entities);
    }

    public void saveGroup(long groupAddress, String groupName, long netKeyIndex) {
        GroupDB groupDB = new GroupDB();
        groupDB.id = groupAddress;
        groupDB.name = groupName;
        groupDB.netkey_index = netKeyIndex;
        mBoxStore.boxFor(GroupDB.class).put(groupDB);
    }

    public GroupDB loadGroup(long groupAddress) {
        return mBoxStore.boxFor(GroupDB.class).get(groupAddress);
    }

    public List<GroupDB> loadGroupListForNetwork(long netKeyIndex) {
        return mBoxStore.boxFor(GroupDB.class)
                .query()
                .equal(GroupDB_.netkey_index, netKeyIndex)
                .build()
                .find();
    }

    public List<GroupDB> loadAllGroups() {
        return mBoxStore.boxFor(GroupDB.class).getAll();
    }

    public void saveModelInGroup(long groupAddress, String nodeMac, long elementAddr, String modelId) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        GroupNodeDB groupNodeDB = box
                .query()
                .equal(GroupNodeDB_.group_address, groupAddress)
                .and()
                .equal(GroupNodeDB_.node_mac, nodeMac)
                .and()
                .equal(GroupNodeDB_.element_address, elementAddr)
                .and()
                .equal(GroupNodeDB_.model_id, modelId)
                .build()
                .findUnique();
        if (groupNodeDB == null) {
            groupNodeDB = new GroupNodeDB();
            groupNodeDB.group_address = groupAddress;
            groupNodeDB.node_mac = nodeMac;
            groupNodeDB.element_address = elementAddr;
            groupNodeDB.model_id = modelId;
            box.put(groupNodeDB);
        }
    }

    public void deleteModelFromGroup(long groupAddr, String nodeMac, long elementAddr, String modelId) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        GroupNodeDB groupNodeDB = box
                .query()
                .equal(GroupNodeDB_.group_address, groupAddr)
                .and()
                .equal(GroupNodeDB_.node_mac, nodeMac)
                .and()
                .equal(GroupNodeDB_.element_address, elementAddr)
                .and()
                .equal(GroupNodeDB_.model_id, modelId)
                .build()
                .findUnique();
        if (groupNodeDB != null) {
            box.remove(groupNodeDB);
        }
    }

    public void deleteElementFromGroup(long groupAddr, String nodeMac, long elementAddr) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        List<GroupNodeDB> entities = box
                .query()
                .equal(GroupNodeDB_.group_address, groupAddr)
                .and()
                .equal(GroupNodeDB_.node_mac, nodeMac)
                .and()
                .equal(GroupNodeDB_.element_address, elementAddr)
                .build()
                .find();
        box.remove(entities);
    }

    public void deleteNodeFromGroup(long groupAddress, String nodeMac) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        GroupNodeDB entity = box
                .query()
                .equal(GroupNodeDB_.group_address, groupAddress)
                .and()
                .equal(GroupNodeDB_.node_mac, nodeMac)
                .build()
                .findUnique();
        if (entity != null) {
            box.remove(entity);
        }
    }

    public void deleteNodesFromGroup(long groupAddress) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        List<GroupNodeDB> entities = box
                .query()
                .equal(GroupNodeDB_.group_address, groupAddress)
                .build()
                .find();
        box.remove(entities);
    }

    public void deleteNodeFromAllGroup(String nodeMac) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        List<GroupNodeDB> entities = box
                .query()
                .equal(GroupNodeDB_.node_mac, nodeMac)
                .build()
                .find();
        box.remove(entities);
    }

    public List<GroupNodeDB> loadGroupNodes(long groupAddress) {
        return mBoxStore.boxFor(GroupNodeDB.class)
                .query()
                .equal(GroupNodeDB_.group_address, groupAddress)
                .build()
                .find();
    }

    public void deleteGroup(long groupAddress) {
        deleteAddress(groupAddress);
        deleteGroupNodes(groupAddress);

        mBoxStore.boxFor(GroupDB.class).remove(groupAddress);
    }

    public void deleteGroupNodes(long groupAddress) {
        Box<GroupNodeDB> box = mBoxStore.boxFor(GroupNodeDB.class);
        List<GroupNodeDB> entities = box.query()
                .equal(GroupNodeDB_.group_address, groupAddress)
                .build()
                .find();
        box.remove(entities);
    }

    public List<FastGroupDB> loadFastGroups() {
        Box<FastGroupDB> box = mBoxStore.boxFor(FastGroupDB.class);
        return box.getAll();
    }

    public long addOrUpdateFastGroup(FastGroupDB entity) {
        Box<FastGroupDB> box = mBoxStore.boxFor(FastGroupDB.class);
        return box.put(entity);
    }

    public void removeFastGroup(long id) {
        Box<FastGroupDB> box = mBoxStore.boxFor(FastGroupDB.class);
        box.remove(id);
    }
}
