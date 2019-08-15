package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

@Entity
public class AppDB {
    @Id(assignable = true)
    public long id; // Key Index

    @Unique
    public String key;

    public long unicast_addr;
}
