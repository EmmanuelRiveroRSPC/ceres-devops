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
        @CompoundIndex(name = "timeslot_index_pgt", def = "{'partition': 1, 'group': 1, 'timeslot': 1}", unique = true)
    }
)
public class Timeslot {
  @Id
  public String id;

  public Integer partition;
  public String group;
  public Instant timeslot;

  public Timeslot(Integer partition, String group, Instant timeslot) {
    this.partition = partition;
    this.group = group;
    this.timeslot = timeslot;
  }
}
