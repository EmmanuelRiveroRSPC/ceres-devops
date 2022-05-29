package com.rackspace.rollupjobs.app.controllers;

import com.rackspace.rollupjobs.app.config.JobConfig;
import com.rackspace.rollupjobs.app.config.JobTimerConfig;
import com.rackspace.rollupjobs.app.config.Properties;
import com.rackspace.rollupjobs.app.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@RestController
public class JobController {
    private final JobConfig jobConfig;
    private final JobTimerConfig jobTimers;
    private final Properties properties;

    JobController(JobConfig jobConfig, JobTimerConfig jobTimers, Properties properties) {
        this.jobConfig = jobConfig;
        this.jobTimers = jobTimers;
        this.properties = properties;
    }

    @PostMapping("/api/job")
    public ResponseEntity<String> getJob(@RequestBody Job job) {
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

    @PutMapping("/api/job")
    public ResponseEntity<String> freeJob(@RequestBody Job job) {
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
