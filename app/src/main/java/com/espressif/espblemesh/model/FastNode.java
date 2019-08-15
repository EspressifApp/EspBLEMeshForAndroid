package com.espressif.espblemesh.model;

public class FastNode {
    private long mAddr;
    private boolean mChecked;

    public void setAddr(long addr) {
        mAddr = addr;
    }

    public long getAddr() {
        return mAddr;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public int hashCode() {
        return ((Long)mAddr).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FastNode) {
            return mAddr == ((FastNode)obj).getAddr();
        }
        return false;
    }
}
