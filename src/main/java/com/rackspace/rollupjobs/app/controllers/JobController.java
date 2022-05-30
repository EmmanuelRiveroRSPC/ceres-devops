package com.rackspace.rollupjobs.app.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.rollupjobs.app.config.JobConfig;
import com.rackspace.rollupjobs.app.config.JobTimerConfig;
import com.rackspace.rollupjobs.app.config.Properties;
import com.rackspace.rollupjobs.app.model.Job;
import com.rackspace.rollupjobs.app.model.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@RestController
public class JobController {
    private final JobConfig jobConfig;
    private final JobTimerConfig jobTimers;
    private final Properties properties;

    JobController(
            JobConfig jobConfig,
            JobTimerConfig jobTimers,
            Properties properties) {
        this.jobConfig = jobConfig;
        this.jobTimers = jobTimers;
        this.properties = properties;
    }

    @GetMapping("/api/job/status")
    public ResponseEntity<String> getJobStatus() throws JsonProcessingException, UnknownHostException {
        JobStatus jobStatus = new JobStatus();
        jobStatus.setJobList(this.jobConfig.jobList());
        jobStatus.setHostName(InetAddress.getLocalHost().getHostName());
        String json = new ObjectMapper().writeValueAsString(jobStatus);
        return ResponseEntity.status(HttpStatus.OK).body(json);
    }

    @PostMapping("/api/job")
    public ResponseEntity<String> getJob(@RequestBody Job job) {
        return claimJobInternal(job);
    }

    @PutMapping("/api/job")
    public ResponseEntity<String> freeJob(@RequestBody Job job) {
        return freeJobInternal(job);
    }

    private ResponseEntity<String> claimJobInternal(@RequestBody Job job) {
        Job searchJob = new Job(job.getPartition(), job.getGroup(), "free");
        int index = this.jobConfig.jobList().indexOf(searchJob);
        if (index > -1) {
            log.info("claimJob: {}", job);
            return claimJob(job, index);
        } else {
            index = this.jobConfig.jobList().indexOf(job);
            if (index > -1) {
                Instant then = this.jobTimers.jobTimers().get(index);
                if (properties.getMaxDownsampleJobDuration().getSeconds() <
                        Duration.between(then, Instant.now()).getSeconds()) {
                    log.info("jobTimed out: {}", job);
                    return claimJob(job, index);
                }
            }
            return ResponseEntity.status(HttpStatus.IM_USED).body("Job is unavailable");
        }
    }

    private ResponseEntity<String> freeJobInternal(@RequestBody Job job) {
        int index = this.jobConfig.jobList().indexOf(job);
        if (index > -1) {
            this.jobConfig.jobList().set(index, new Job(job.getPartition(), job.getGroup(), "free"));
            log.info("Freeing job: {}", job);
            return ResponseEntity.status(HttpStatus.OK).body("Job is free");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body("Job is not found, but ok, I'll let it slide...");
        }
    }

    private ResponseEntity<String> claimJob(@RequestBody Job job, int index) {
        this.jobConfig.jobList().set(index, new Job(job.getPartition(), job.getGroup(), job.getStatus()));
        this.jobTimers.jobTimers().set(index, Instant.now());
        return ResponseEntity.status(HttpStatus.OK).body("Job is assigned");
    }
}
