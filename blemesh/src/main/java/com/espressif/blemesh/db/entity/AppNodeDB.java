package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class AppNodeDB {
    @Id
    public long id;

    public long app_key_index;

    public String node_mac;
}
