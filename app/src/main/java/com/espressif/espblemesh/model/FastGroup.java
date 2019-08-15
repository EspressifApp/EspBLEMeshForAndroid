package com.espressif.espblemesh.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FastGroup {
    private long mId;
    private String mName;

    private long mAddr;

    private final Set<Long> mNodeAddrs = new HashSet<>();

    public void setId(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setAddr(long addr) {
        mAddr = addr;
    }

    public long getAddr() {
        return mAddr;
    }

    public void setNodeAddrs(Collection<Long> nodeAddrs) {
        synchronized (mNodeAddrs) {
            mNodeAddrs.clear();
            mNodeAddrs.addAll(nodeAddrs);
        }
    }

    public List<Long> getNodeAddrs() {
        synchronized (mNodeAddrs) {
            return new ArrayList<>(mNodeAddrs);
        }
    }

    public void addNodeAddrs(Collection<Long> nodeAddrs) {
        synchronized (mNodeAddrs) {
            mNodeAddrs.addAll(nodeAddrs);
        }
    }

    public void removeNodeAddrs(Collection<Long> nodeAddrs) {
        synchronized (mNodeAddrs) {
            mNodeAddrs.removeAll(nodeAddrs);
        }
    }
}
