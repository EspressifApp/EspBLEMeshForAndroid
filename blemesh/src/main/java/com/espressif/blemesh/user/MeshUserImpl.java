package com.espressif.blemesh.user;

import android.util.ArrayMap;
import android.util.LongSparseArray;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.db.entity.AppDB;
import com.espressif.blemesh.db.entity.AppNodeDB;
import com.espressif.blemesh.db.entity.ElementDB;
import com.espressif.blemesh.db.entity.GroupDB;
import com.espressif.blemesh.db.entity.GroupNodeDB;
import com.espressif.blemesh.db.entity.ModelDB;
import com.espressif.blemesh.db.entity.NetworkDB;
import com.espressif.blemesh.db.entity.NodeDB;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Element;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.utils.DBUtils;

import java.util.ArrayList;
import java.util.List;

class MeshUserImpl {
    private final LongSparseArray<Network> mNetworks = new LongSparseArray<>();
    private final LongSparseArray<App> mApps = new LongSparseArray<>();
    private final LongSparseArray<Group> mGroups = new LongSparseArray<>();
    private final ArrayMap<String, Node> mNodes = new ArrayMap<>();

    void reload() {
        loadNetworksFromDB();
        loadAppsFromDB();
        loadGroupsFromDB();
        loadNodesFromDB();
    }

    void loadNetworksFromDB() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<NetworkDB> networkDBS = dbManager.loadAllNetworks();
        synchronized (mNetworks) {
            mNetworks.clear();

            for (NetworkDB networkDB : networkDBS) {
                Network network = DBUtils.getNetworkWithDB(networkDB);
                loadNetworkGroupsFromDB(network);
                loadNetworkNodesFromDB(network);

                mNetworks.put(network.getKeyIndex(), network);
            }
        }
    }

    private void loadNetworkGroupsFromDB(Network network) {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<GroupDB> groupDBS = dbManager.loadGroupListForNetwork(network.getKeyIndex());
        for (GroupDB groupDB : groupDBS) {
            network.addGroup(groupDB.id);
        }
    }

    private void loadNetworkNodesFromDB(Network network) {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<NodeDB> nodeDBS = dbManager.loadNodeListForNetwork(network.getKeyIndex());
        for (NodeDB nodeDB : nodeDBS) {
            network.addNode(nodeDB.mac);
        }
    }

    void addNetwork(Network network) {
        synchronized (mNetworks) {
            if (mNetworks.get(network.getKeyIndex()) == null) {
                mNetworks.put(network.getKeyIndex(), network);
            }
        }
    }

    void removeNetwork(long netkeyIndex) {
        synchronized (mNetworks) {
            Network network = getNetworkForKeyIndex(netkeyIndex);
            if (network == null) {
                return;
            }

            mNetworks.remove(netkeyIndex);

            for (long groupAddr : network.getGroupAddressList()) {
                removeGroup(groupAddr);
            }

            for (String nodeMac : network.getNodeMacList()) {
                removeNode(nodeMac);
            }
        }
    }

    List<Network> getNetworkList() {
        synchronized (mNetworks) {
            List<Network> list = new ArrayList<>(mNetworks.size());
            for (int i = 0; i < mNetworks.size(); i++) {
                list.add(mNetworks.valueAt(i));
            }
            return list;
        }
    }

    Network getNetworkForKeyIndex(long netKeyIndex) {
        synchronized (mNetworks) {
            return mNetworks.get(netKeyIndex);
        }
    }

    void loadAppsFromDB() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<AppDB> appDBS = dbManager.loadAllApps();
        synchronized (mApps) {
            mApps.clear();

            for (AppDB appDB : appDBS) {
                App app = DBUtils.getAppWithDB(appDB);
                mApps.put(app.getKeyIndex(), app);
            }
        }
    }

    void addApp(App app) {
        synchronized (mApps) {
            if (mApps.indexOfKey(app.getKeyIndex()) < 0) {
                mApps.put(app.getKeyIndex(), app);
            }
        }
    }

    void removeApp(long appKeyIndex) {
        synchronized (mApps) {
            mApps.remove(appKeyIndex);
        }
    }

    List<App> getAppList() {
        synchronized (mApps) {
            List<App> list = new ArrayList<>(mApps.size());
            for (int i = 0; i < mApps.size(); i++) {
                list.add(mApps.valueAt(i));
            }
            return list;
        }
    }

    LongSparseArray<App> getAppSparseArray() {
        synchronized (mApps) {
            LongSparseArray<App> array = new LongSparseArray<>(mApps.size());
            for (int i = 0; i < mApps.size(); i++) {
                array.append(mApps.keyAt(i), mApps.valueAt(i));
            }
            return array;
        }
    }

    App getAppForKeyIndex(long appKeyIndex) {
        synchronized (mApps) {
            return mApps.get(appKeyIndex);
        }
    }

    void loadGroupsFromDB() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<GroupDB> groupDBS = dbManager.loadAllGroups();
        synchronized (mGroups) {
            mGroups.clear();

            for (GroupDB groupDB : groupDBS) {
                Group group = DBUtils.getGroupWithDB(groupDB);
                loadGroupNodesFromDB(group);

                mGroups.put(group.getAddress(), group);
            }
        }
    }

    private void loadGroupNodesFromDB(Group group) {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<GroupNodeDB> groupNodeDBS = dbManager.loadGroupNodes(group.getAddress());
        for (GroupNodeDB groupNodeDB : groupNodeDBS) {
            group.addModel(groupNodeDB.node_mac, groupNodeDB.element_address, groupNodeDB.model_id);
        }
    }

    List<Group> getGroupList() {
        synchronized (mGroups) {
            List<Group> list = new ArrayList<>(mGroups.size());
            for (int i = 0; i < mGroups.size(); i++) {
                list.add(mGroups.valueAt(i));
            }
            return list;
        }
    }

    List<Group> getGroupListForNetwork(long netKeyIndex) {
        synchronized (mGroups) {
            List<Group> groups = new ArrayList<>();
            for (int i = 0; i < mGroups.size(); i++) {
                Group group = mGroups.valueAt(i);
                if (group.getNetKeyIndex() == netKeyIndex) {
                    groups.add(group);
                }
            }

            return groups;
        }
    }

    Group getGroupForAddress(long groupAddress) {
        synchronized (mGroups) {
            return mGroups.get(groupAddress);
        }
    }

    void loadNodesFromDB() {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<NodeDB> nodeDBS = dbManager.loadAllNodes();
        synchronized (mNodes) {
            for (NodeDB nodeDB : nodeDBS) {
                Node node = DBUtils.getNodeWithDB(nodeDB);
                loadNodeElementsFromDB(node);
                loadNodeAppFromDB(node);

                mNodes.put(node.getMac(), node);
            }
        }
    }

    void loadNodeElementsFromDB(Node node) {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<ElementDB> elementDBS = dbManager.loadElementsForNode(node.getMac());
        for (ElementDB elementDB : elementDBS) {
            Element element = DBUtils.getElementWithDB(elementDB);
            loadElementModelsFromDB(element);
            node.addElement(element);
        }
    }

    void loadNodeAppFromDB(Node node) {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<AppNodeDB> appNodeDBS = dbManager.loadAppNodeForNodeMac(node.getMac());
        for (AppNodeDB appNodeDB : appNodeDBS) {
            node.addAppKeyIndex(appNodeDB.app_key_index);
        }
    }

    void loadElementModelsFromDB(Element element) {
        MeshObjectBox dbManager = MeshObjectBox.getInstance();

        List<ModelDB> modelDBS = dbManager.loadModelsForElement(element.getUnicastAddress());
        for (ModelDB modelDB : modelDBS) {
            Model model = DBUtils.getModelWithDB(modelDB);
            element.addModel(model);
        }
    }

    Node getNodeForMac(String nodeMac) {
        synchronized (mNodes) {
            return mNodes.get(nodeMac);
        }
    }

    void addGroup(Group group) {
        synchronized (mGroups) {
            mGroups.put(group.getAddress(), group);
        }
    }

    void removeGroup(long groupAddress) {
        synchronized (mGroups) {
            mGroups.remove(groupAddress);
        }
    }

    void removeNode(String nodeMac) {
        synchronized (mGroups) {
            List<Group> groups = getGroupList();
            for (Group group : groups) {
                group.removeNode(nodeMac);
            }
        }

        synchronized (mNetworks) {
            List<Network> networks = getNetworkList();
            for (Network network : networks) {
                network.removeNode(nodeMac);
            }
        }

        synchronized (mNodes) {
            mNodes.remove(nodeMac);
        }

    }
}
