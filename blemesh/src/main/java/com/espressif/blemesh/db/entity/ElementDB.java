package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class ElementDB {
    @Id(assignable = true)
    public long id; // Unicast Address

    @Index
    public String node_mac;

    public long loc;
}
