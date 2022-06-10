/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rackspace.mongobuffer.app.services;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import com.rackspace.mongobuffer.app.config.BufferConfig;
import com.rackspace.mongobuffer.app.config.Properties;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WriteService {

  private final BufferConfig bufferConfig;
  private final MongoClient mongoClient;


  @Autowired
  public WriteService(BufferConfig bufferConfig, MongoClient mongoClient) {
    this.bufferConfig = bufferConfig;
    this.mongoClient = mongoClient;
  }

  public BulkWriteResult writeDownsamplings() {
    MongoDatabase database = this.mongoClient.getDatabase("ceres");
    MongoCollection<Document> collection = database.getCollection("downsampling");
    List<WriteModel<Document>> writeOperations = new ArrayList<>();

    this.bufferConfig.downsamplingSet().forEach(d -> {
      List<Bson> filters = new ArrayList<>();
      filters.add(Filters.eq("partition", d.getPartition()));
      filters.add(Filters.eq("group", d.getGroup()));
      filters.add(Filters.eq("timeslot", d.getTimeslot()));
      filters.add(Filters.eq("setHash", d.getSetHash()));
      Bson andComparison = Filters.and(filters);

      writeOperations.add(new DeleteOneModel<>(andComparison));
      writeOperations.add(new InsertOneModel<>(
          new Document("partition", d.getPartition())
              .append("group", d.getGroup())
              .append("timeslot", d.getTimeslot())
              .append("setHash", d.getSetHash())));
        }
    );
    BulkWriteResult bulkWriteResult = collection.bulkWrite(writeOperations);
    log.info("downsampling bulk write result: {}", bulkWriteResult);
    return bulkWriteResult;
  }

  public BulkWriteResult writeTimeslots() {
    MongoDatabase database = this.mongoClient.getDatabase("ceres");
    MongoCollection<Document> collection = database.getCollection("timeslot");
    List<WriteModel<Document>> writeOperations = new ArrayList<>();

    this.bufferConfig.timeslotSet().forEach(d -> {
          List<Bson> filters = new ArrayList<>();
          filters.add(Filters.eq("partition", d.getPartition()));
          filters.add(Filters.eq("group", d.getGroup()));
          filters.add(Filters.eq("timeslot", d.getTimeslot()));
          Bson andComparison = Filters.and(filters);

          writeOperations.add(new DeleteOneModel<>(andComparison));
          writeOperations.add(new InsertOneModel<>(
              new Document("partition", d.getPartition())
                  .append("group", d.getGroup())
                  .append("timeslot", d.getTimeslot())));
        }
    );
    BulkWriteResult bulkWriteResult = collection.bulkWrite(writeOperations);
    log.info("timeslot bulk write result: {}", bulkWriteResult);
    return bulkWriteResult;
  }



}
