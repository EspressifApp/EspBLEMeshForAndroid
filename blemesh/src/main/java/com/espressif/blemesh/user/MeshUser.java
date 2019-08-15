package com.espressif.blemesh.user;

import android.util.LongSparseArray;

import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Element;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;

import java.util.List;

public enum MeshUser {
    Instance;

    private final MeshUserImpl mImpl = new MeshUserImpl();

    public void reload() {
        mImpl.reload();
    }

    public void loadNetworksFromDB() {
        mImpl.loadNetworksFromDB();
    }

    public void loadAppsFromDB() {
        mImpl.loadAppsFromDB();
    }

    public void loadGroupsFromDB() {
        mImpl.loadGroupsFromDB();
    }

    public void loadNodesFromDB() {
        mImpl.loadNodesFromDB();
    }

    public void loadNodeAppFromDB(Node node) {
        mImpl.loadNodeAppFromDB(node);
    }

    public void loadNodeElementsFromDB(Node node) {
        mImpl.loadNodeElementsFromDB(node);
    }

    public void loadElementModelsFromDB(Element element) {
        mImpl.loadElementModelsFromDB(element);
    }

    public void addNetwork(Network network) {
        mImpl.addNetwork(network);
    }

    public void removeNetwork(long netkeyIndex) {
        mImpl.removeNetwork(netkeyIndex);
    }

    public List<Network> getNetworkList() {
        return mImpl.getNetworkList();
    }

    public Network getNetworkForKeyIndex(long netKeyIndex) {
        return mImpl.getNetworkForKeyIndex(netKeyIndex);
    }

    public void addApp(App app) {
        mImpl.addApp(app);
    }

    public void removeApp(long appKeyIndex) {
        mImpl.removeApp(appKeyIndex);
    }

    public List<App> getAppList() {
        return mImpl.getAppList();
    }

    public LongSparseArray<App> getAppSparseArray() {
        return mImpl.getAppSparseArray();
    }

    public App getAppForKeyIndex(long appKeyIndex) {
        return mImpl.getAppForKeyIndex(appKeyIndex);
    }

    public List<Group> getGroupList() {
        return mImpl.getGroupList();
    }

    public List<Group> getGroupListForNetwork(long netKeyIndex) {
        return mImpl.getGroupListForNetwork(netKeyIndex);
    }

    public Group getGroupForAddress(long groupAddress) {
        return mImpl.getGroupForAddress(groupAddress);
    }

    public Node getNodeForMac(String nodeMac) {
        return mImpl.getNodeForMac(nodeMac);
    }

    public void addGroup(Group group) {
        mImpl.addGroup(group);
    }

    public void removeGroup(long groupAddress) {
        mImpl.removeGroup(groupAddress);
    }

    public void removeNode(String nodeMac) {
        mImpl.removeNode(nodeMac);
    }
}
