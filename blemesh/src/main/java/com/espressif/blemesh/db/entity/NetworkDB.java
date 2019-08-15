package com.espressif.blemesh.db.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class NetworkDB {
    @Id(assignable = true)
    public long id; // Key Index

    public String key;

    public String name;

    public long iv_index;

    public long seq;
}
