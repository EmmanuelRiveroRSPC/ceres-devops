package com.rackspace.ceres.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Data
@Document
public class Pending {
    @Id
    public String id;

    public Integer partition;
    public String group;
    public Set<String> timeslots;

    public Pending(Integer partition, String group) {
        this.partition = partition;
        this.group = group;
        this.timeslots = new HashSet<>();
    }
}
