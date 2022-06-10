package com.rackspace.mongobuffer.app.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.mongobuffer.app.config.BufferConfig;
import com.rackspace.mongobuffer.app.config.Properties;
import com.rackspace.mongobuffer.app.model.Downsampling;
import com.rackspace.mongobuffer.app.model.Timeslot;
import com.rackspace.mongobuffer.app.services.WriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class BufferController {
    private final BufferConfig bufferConfig;
    private final Properties properties;
    private final WriteService writeService;

    BufferController(BufferConfig bufferConfig, Properties properties, WriteService writeService) {
        this.bufferConfig = bufferConfig;
        this.properties = properties;
        this.writeService = writeService;
    }

    @GetMapping("/api/status")
    public ResponseEntity<String> getStatus() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(bufferConfig.downsamplingSet());
        return ResponseEntity.status(HttpStatus.OK).body(json);
    }

    @PostMapping("/api/downsampling")
    public synchronized ResponseEntity<String> bufferDownsampling(@RequestBody Downsampling ds) {
        log.trace("Got downsampling {}", ds);
        bufferConfig.downsamplingSet().add(ds);
        bufferConfig.timeslotSet().add(new Timeslot(ds.getPartition(), ds.getGroup(), ds.getTimeslot()));
        log.info("downsamplingSet size: {}", bufferConfig.downsamplingSet().size());
        log.trace("timeslotSet size: {}", bufferConfig.timeslotSet().size());

        if (bufferConfig.downsamplingSet().size() > this.properties.getMaxNum()) {
            this.writeService.writeDownsamplings();
            this.writeService.writeTimeslots();
            this.bufferConfig.downsamplingSet().clear();
            this.bufferConfig.timeslotSet().clear();
        }
        return ResponseEntity.status(HttpStatus.OK).body(
            String.format("{ \"num_not_flushed:\": %d }", this.bufferConfig.downsamplingSet().size()));
    }
}
