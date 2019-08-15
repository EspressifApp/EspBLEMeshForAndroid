package com.espressif.blemesh.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element {
    private long mUnicastAddress;
    private long mLoc;
    private String mNodeMac;

    private final Map<String, Model> mModels = new HashMap<>();

    public long getUnicastAddress() {
        return mUnicastAddress;
    }

    public void setUnicastAddress(long unicastAddress) {
        mUnicastAddress = unicastAddress;
    }

    public long getLoc() {
        return mLoc;
    }

    public void setLoc(long loc) {
        mLoc = loc;
    }

    public String getNodeMac() {
        return mNodeMac;
    }

    public void setNodeMac(String nodeMac) {
        mNodeMac = nodeMac;
    }

    public void addModel(Model model) {
        synchronized (mModels) {
            mModels.put(model.getId(), model);
        }
    }

    public void removeModel(Model model) {
        synchronized (mModels) {
            mModels.remove(model.getId());
        }
    }

    public List<Model> getModelList() {
        synchronized (mModels) {
            return new ArrayList<>(mModels.values());
        }
    }

    public Model getModeForId(String modelId) {
        synchronized (mModels) {
            return mModels.get(modelId);
        }
    }
}
