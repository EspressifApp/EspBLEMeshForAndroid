package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class GroupDB {
    @Id(assignable = true)
    public long id; // Group Address

    public String name;

    public long netkey_index;
}
