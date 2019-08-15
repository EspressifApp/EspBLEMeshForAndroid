package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class ModelDB {
    @Id
    public Long id;

    public String model_id;

    @Index
    public long element_address;

    public String node_mac;

    public long app_key_index;
}
