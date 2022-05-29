package com.rackspace.rollupjobs.app.config;

import com.rackspace.rollupjobs.app.model.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Configuration
public class JobConfig {
    private final Properties properties;

    public JobConfig(Properties properties) {
        this.properties = properties;
    }

    @Bean
    public List<Job> jobList() {
        List<Job> jobList = new ArrayList<>();
        properties.getGranularities().stream().map(granularity -> granularity.getPartitionWidth().toString())
                .forEach(group -> IntStream.rangeClosed(0, properties.getPartitions() - 1)
                        .forEach(partition -> jobList.add(new Job(partition, group, "free"))));
        return jobList;
    }
}
