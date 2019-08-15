package com.espressif.blemesh.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Group {
    private long mAddress;
    private String mName;
    private long mNetKeyIndex;

    private final Object mLock = new Object();
    private HashMap<String, Set<Long>> mNodeElementsMap = new HashMap<>();
    private HashMap<Long, Set<String>> mElementModelsMap = new HashMap<>();

    public long getAddress() {
        return mAddress;
    }

    public void setAddress(long groupAddress) {
        mAddress = groupAddress;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public long getNetKeyIndex() {
        return mNetKeyIndex;
    }

    public void setNetKeyIndex(long netKeyIndex) {
        mNetKeyIndex = netKeyIndex;
    }

    public void addModel(String nodeMac, long elementAddr, String modelId) {
        synchronized (mLock) {
            Set<Long> elementSet = mNodeElementsMap.get(nodeMac);
            if (elementSet == null) {
                elementSet = new HashSet<>();
                mNodeElementsMap.put(nodeMac, elementSet);
            }
            elementSet.add(elementAddr);

            Set<String> modelSet = mElementModelsMap.get(elementAddr);
            if (modelSet == null) {
                modelSet = new HashSet<>();
                mElementModelsMap.put(elementAddr, modelSet);
            }
            modelSet.add(modelId);
        }
    }

    public void removeModel(String nodeMac, long elementAddr, String modelId) {
        synchronized (mLock) {
            Set<String> modelSet = mElementModelsMap.get(elementAddr);
            if (modelSet != null) {
                modelSet.remove(modelId);

                if (modelSet.isEmpty()) {
                    mElementModelsMap.remove(elementAddr);

                    Set<Long> elementSet = mNodeElementsMap.get(nodeMac);
                    elementSet.remove(elementAddr);
                    if (elementSet.isEmpty()) {
                        mNodeElementsMap.remove(nodeMac);
                    }
                }
            }
        }
    }

    public void removeNode(String nodeMac) {
        synchronized (mLock) {
            Set<Long> elementSet = mNodeElementsMap.remove(nodeMac);
            if (elementSet != null) {
                for (Long elementAddr : elementSet) {
                    mElementModelsMap.remove(elementAddr);
                }
            }
        }
    }

    public boolean hasNode(String nodeMac) {
        synchronized (mLock) {
            return mNodeElementsMap.containsKey(nodeMac);
        }
    }

    public boolean hasModel(long elementAddr, String modelId) {
        synchronized (mLock) {
            Set<String> modelSet = mElementModelsMap.get(elementAddr);
            return modelSet != null && modelSet.contains(modelId);
        }
    }

    public List<String> getNodeMacList() {
        synchronized (mLock) {
            return new ArrayList<>(mNodeElementsMap.keySet());
        }
    }
}
