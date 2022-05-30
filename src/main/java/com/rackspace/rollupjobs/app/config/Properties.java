package com.rackspace.rollupjobs.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("jobs")
@Component
@Data
@Validated
public class Properties {
    /**
     * Specifies how many partitions should be used when tracking ingested data for downsampling.
     */
    int partitions;

    /**
     * List of partition widths
     */
    List<Granularity> granularities;

    /**
     * Max time a downsampling job can be locked
     */
    Duration maxDownsampleJobDuration;

    /**
     * Cluster IP of ourselves to ping the status of our peer
     */
    String clusterIp;

    @Data
    public static class Granularity {
        /**
         * The width of time slots used for partitioning data
         */
        Duration partitionWidth;
    }
}
