package com.rackspace.mongobuffer.app.config;

import com.rackspace.mongobuffer.app.model.Downsampling;
import com.rackspace.mongobuffer.app.model.Timeslot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class BufferConfig {

  public BufferConfig() {
  }

  @Bean
  public Set<Downsampling> downsamplingSet() {
    Set<Downsampling> downsamplings = new HashSet<>();
    return downsamplings;
  }

  @Bean
  public Set<Timeslot> timeslotSet() {
    Set<Timeslot> timeslots = new HashSet<>();
    return timeslots;
  }
}
