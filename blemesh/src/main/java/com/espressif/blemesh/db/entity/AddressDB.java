package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class AddressDB {
    @Id(assignable = true)
    public long id; // Address
}
