package com.rackspace.rollupjobs.app.model;

import lombok.Data;

import java.util.List;

@Data
public class JobStatus {
    List<Job> jobList;
    String hostName;
}
