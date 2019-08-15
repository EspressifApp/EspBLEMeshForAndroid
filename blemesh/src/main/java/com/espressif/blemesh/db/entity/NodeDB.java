package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Unique;

@Entity
public class NodeDB {
    @Id
    public long id;

    @Index
    @Unique
    public String mac;

    public String uuid;

    public String key;

    public String name;

    public long unicast_addr;

    public int element_count;

    public long netkey_index;

    public Long cid;

    public Long pid;

    public Long vid;

    public Long crpl;

    public Long features;
}
