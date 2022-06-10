package com.rackspace.mongobuffer.app.config;

import com.rackspace.mongobuffer.app.model.Downsampling;
import com.rackspace.mongobuffer.app.model.Timeslot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class BufferConfig {

  private final Set<Downsampling> downsamplings;
  private final Set<Timeslot> timeslots;

  public BufferConfig() {
    this.downsamplings = new HashSet<>();
    this.timeslots = new HashSet<>();
  }

  @Bean
  public Set<Downsampling> downsamplingSet() {
    return downsamplings;
  }

  @Bean
  public Set<Timeslot> timeslotSet() {
    return timeslots;
  }
}
