package com.espressif.blemesh.db.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class FastGroupDB {
    @Id
    public long id;

    public String name;

    public long addr;

    public String nodes;

    public List<Long> getNodeAddrListForNodes() {
        if (nodes == null) {
            return Collections.emptyList();
        }

        String[] array = nodes.split(",");
        List<Long> result = new ArrayList<>(array.length);
        for (String str : array) {
            result.add(Long.parseLong(str));
        }
        return result;
    }

    public void setNodeForList(List<Long> list) {
        if (list == null || list.isEmpty()) {
            nodes = null;
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(i));
            if (i < list.size() - 1) {
                builder.append(",");
            }
        }

        nodes = builder.toString();
    }
}
