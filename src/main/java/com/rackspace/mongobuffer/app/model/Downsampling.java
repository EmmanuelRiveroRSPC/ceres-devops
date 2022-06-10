package com.rackspace.mongobuffer.app.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document
@CompoundIndexes(
    {
        @CompoundIndex(name = "downsampling_index_pgts", def = "{'partition': 1, 'group': 1, 'timeslot': 1, 'setHash': 1}", unique = true)
    }
)
public class Downsampling {
    @Id
    public String id;

    public Integer partition;
    public String group;
    public Instant timeslot;
    public String setHash;

    public Downsampling(Integer partition, String group, Instant timeslot, String setHash) {
        this.partition = partition;
        this.group = group;
        this.timeslot = timeslot;
        this.setHash = setHash;
    }
}
