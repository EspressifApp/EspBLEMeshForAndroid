package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class GroupNodeDB {
    @Id
    public long id;

    public long group_address;

    public String node_mac;

    public long element_address;

    public String model_id;
}
