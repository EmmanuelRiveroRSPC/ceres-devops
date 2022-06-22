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

package com.rackspace.ceres.app.services;

import com.rackspace.ceres.app.config.DownsampleProperties;
import com.rackspace.ceres.app.config.DownsampleProperties.Granularity;
import com.rackspace.ceres.app.downsample.*;
import com.rackspace.ceres.app.model.PendingDownsampleSet;
import com.rackspace.ceres.app.utils.DateTimeUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.rackspace.ceres.app.utils.DateTimeUtils.*;

@Service
@Slf4j
@Profile("downsample")
public class DownsampleProcessor {

  private final DownsampleProperties properties;
  private final DownsampleTrackingService trackingService;
  private final DelayedTrackingService delayedTrackingService;
  private final QueryService queryService;
  private final DataWriteService dataWriteService;
  private final Timer meterTimer;
  private final ScheduledExecutorService executor;

  @Autowired
  public DownsampleProcessor(DownsampleProperties properties,
                             DownsampleTrackingService trackingService,
                             DelayedTrackingService delayedTrackingService,
                             QueryService queryService,
                             DataWriteService dataWriteService,
                             MeterRegistry meterRegistry,
                             ScheduledExecutorService executor) {
    this.properties = properties;
    this.trackingService = trackingService;
    this.delayedTrackingService = delayedTrackingService;
    this.queryService = queryService;
    this.dataWriteService = dataWriteService;
    this.meterTimer = meterRegistry.timer("downsampling.delay");
    this.executor = executor;
  }

  @PostConstruct
  public void setupSchedulers() {
    if (properties.getGranularities() == null ||
        properties.getGranularities().isEmpty()) {
      throw new IllegalStateException("Granularities are not configured!");
    }
    long initialDelay = properties.getInitialProcessingDelay().getSeconds();
    executor.schedule(this::initializeRedisJobs, 1, TimeUnit.SECONDS);
    executor.schedule(this::initializeJobs, initialDelay, TimeUnit.SECONDS);
    executor.schedule(this::initializeDelayedTimeslotJobs, initialDelay, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    executor.shutdown();
  }

  private void initializeRedisJobs() {
    getPartitionWidths(properties.getGranularities())
        .forEach(width -> IntStream.rangeClosed(0, properties.getPartitions() - 1)
            .forEach((partition) -> trackingService.freeJob(partition, width).subscribe()));
    IntStream.rangeClosed(0, properties.getPartitions() - 1)
        .forEach((partition) -> delayedTrackingService.freeJob(partition).subscribe());
  }

  private void initializeJobs() {
    log.info("Start downsampling jobs");
    log.info("Downsampling configuration parameters");
    log.info("=====================================");
    log.info("downsample-spread-period: {}", properties.getDownsampleSpreadPeriod().getSeconds());
    log.info("max-concurrent-downsample-hashes: {}", properties.getMaxConcurrentDownsampleHashes());
    log.info("max-downsample-job-duration: {}", properties.getMaxDownsampleJobDuration().getSeconds());
    log.info("downsample-delayed-timeslot-period: {}", properties.getDownsampleDelayedTimeslotPeriod().getSeconds());
    log.info("=====================================");

    getPartitionWidths(properties.getGranularities())
        .forEach(width -> IntStream.rangeClosed(0, properties.getPartitions() - 1)
            .forEach((partition) -> executor.schedule(() ->
                    processJob(partition, width),
                randomDelay(properties.getDownsampleSpreadPeriod().getSeconds()), TimeUnit.SECONDS)));
  }

  private void initializeDelayedTimeslotJobs() {
    IntStream.rangeClosed(0, properties.getPartitions() - 1)
        .forEach((partition) -> executor.schedule(
            () -> processDelayedTimeslotJob(partition),
            properties.getDownsampleDelayedTimeslotPeriod().getSeconds(), TimeUnit.SECONDS));
  }

  private void processJob(int partition, String group) {
    log.trace("processJob {} {}", partition, group);
    trackingService.claimJob(partition, group)
            .flatMap(status -> status.equals("free") ?
                processTimeSlot(partition, group).then(trackingService.freeJob(partition, group)) : Flux.empty()
            ).subscribe();
    executor.schedule(() -> processJob(partition, group),
        randomDelay(properties.getDownsampleSpreadPeriod().getSeconds()), TimeUnit.SECONDS);
  }

  private void processDelayedTimeslotJob(int partition) {
    log.trace("processDelayedTimeslotJob {}", partition);
    delayedTrackingService.claimJob(partition)
        .flatMap(status -> status.equals("free") ?
            processDelayedTimeSlot(partition)
                .then(delayedTrackingService.freeJob(partition)) : Flux.empty()
        ).subscribe();
    executor.schedule(() -> processDelayedTimeslotJob(partition),
        properties.getDownsampleDelayedTimeslotPeriod().getSeconds(), TimeUnit.SECONDS);
  }

  private Flux<?> processTimeSlot(int partition, String group) {
    log.trace("processTimeSlot {} {}", partition, group);
    return trackingService.getTimeSlot(partition, group)
        .flatMapMany(ts -> {
          long timeslot = Long.parseLong(ts);
          log.info("Got timeslot: {} {} {}", partition, group, epochToLocalDateTime(timeslot));
          return trackingService.getDownsampleSets(timeslot, partition)
              .name("processTimeSlot")
              .tag("partition", String.valueOf(partition))
              .tag("group", group)
              .metrics()
              .flatMap(downsampleSet -> processDownsampleSet(downsampleSet, partition, group),
                  properties.getMaxConcurrentDownsampleHashes())
              .then(trackingService.deleteTimeslot(partition, group, timeslot))
              .doOnError(Throwable::printStackTrace);
        });
  }
  private Flux<?> processDelayedTimeSlot(int partition) {
    return Flux.fromIterable(getPartitionWidths(properties.getGranularities()))
        .flatMap(group -> delayedTrackingService.getDelayedTimeSlots(partition, group)
            .map(tsString -> DelayedTrackingService.buildDownsampleSet(partition, group, tsString))
            .name("processDelayedTimeSlot")
            .tag("partition", String.valueOf(partition))
            .tag("group", group)
            .metrics()
            .flatMap(downsampleSet -> Mono.from(processDownsampleSet(downsampleSet, partition, group))
                    .then(delayedTrackingService.deleteDelayedTimeslot(
                        partition, group, DelayedTrackingService.encodeDelayedTimeslot(downsampleSet))),
                properties.getMaxConcurrentDownsampleHashes())
            .doOnError(Throwable::printStackTrace)
        );
  }

  private Publisher<?> processDownsampleSet(PendingDownsampleSet pendingSet, int partition, String group) {
    log.trace("processDownsampleSet {} {} {}", pendingSet, partition, group);
    Duration downsamplingDelay = Duration.between(pendingSet.getTimeSlot(), Instant.now());
    this.meterTimer.record(downsamplingDelay.getSeconds(), TimeUnit.SECONDS);

    final Flux<ValueSet> data = queryService.queryRawWithSeriesSet(
        pendingSet.getTenant(),
        pendingSet.getSeriesSetHash(),
        pendingSet.getTimeSlot(),
        pendingSet.getTimeSlot().plus(Duration.parse(group))
    );

    List<Granularity> granularities = DateTimeUtils.filterGroupGranularities(group, properties.getGranularities());
    return
        downsampleData(data,
            pendingSet.getTenant(),
            pendingSet.getSeriesSetHash(),
            granularities.iterator()
        )
            .name("downsample.set")
            .tag("partition", String.valueOf(partition))
            .tag("group", group)
            .metrics()
            .doOnSuccess(o ->
                log.trace("Completed downsampling of set: {} timeslot: {} time: {} partition: {} group: {}",
                    pendingSet.getSeriesSetHash(),
                    pendingSet.getTimeSlot().getEpochSecond(),
                    epochToLocalDateTime(pendingSet.getTimeSlot().getEpochSecond()),
                    partition, group))
            .checkpoint();
  }

  /**
   * Downsample the given data into the next granularity, stores that data,
   * and recurses until the remaining granularities are processed.
   *
   * @param data          a flux of either raw {@link SingleValueSet}s or
   *                      aggregated {@link AggregatedValueSet}s from the prior granularity.
   * @param tenant        the tenant of the pending downsample set
   * @param seriesSet     the series-set of the pending downsample set
   * @param granularities remaining granularities to process
   * @return a mono that completes when the aggregated data has been stored
   */
  public Mono<?> downsampleData(Flux<? extends ValueSet> data,
                                String tenant,
                                String seriesSet,
                                Iterator<Granularity> granularities) {
    if (!granularities.hasNext()) {
      // end of the recursion so pop back out
      return Mono.empty();
    }

    final Granularity granularity = granularities.next();
    final TemporalNormalizer normalizer = new TemporalNormalizer(granularity.getWidth());

    final Flux<AggregatedValueSet> aggregated =
        data
            .doOnNext(valueSet -> log.trace("Aggregating {} into granularity={}", valueSet, granularity))
            // group the incoming data by granularity-time-window
            .windowUntilChanged(
                valueSet -> valueSet.getTimestamp().with(normalizer), Instant::equals)
            // ...and then do the aggregation math on those
            .concatMap(valueSetFlux ->
                valueSetFlux.collect(ValueSetCollectors.gaugeCollector(granularity.getWidth())));

    // expand the aggregated volume-sets into individual data points to be stored
    final Flux<DataDownsampled> expanded = expandAggregatedData(aggregated, tenant, seriesSet);

    return dataWriteService.storeDownsampledData(expanded)
        .then(
            // ...and recurse into remaining granularities
            downsampleData(aggregated, tenant, seriesSet, granularities)
        )
        .checkpoint();
  }

  public Flux<DataDownsampled> expandAggregatedData(Flux<AggregatedValueSet> aggs, String tenant, String seriesSet) {
    return aggs.flatMap(agg -> Flux.just(
        data(tenant, seriesSet, agg).setAggregator(Aggregator.sum).setValue(agg.getSum()),
        data(tenant, seriesSet, agg).setAggregator(Aggregator.min).setValue(agg.getMin()),
        data(tenant, seriesSet, agg).setAggregator(Aggregator.max).setValue(agg.getMax()),
        data(tenant, seriesSet, agg).setAggregator(Aggregator.avg).setValue(agg.getAverage())
    ));
  }

  private static DataDownsampled data(String tenant, String seriesSet, AggregatedValueSet agg) {
    return new DataDownsampled()
        .setTs(agg.getTimestamp())
        .setGranularity(agg.getGranularity())
        .setTenant(tenant)
        .setSeriesSetHash(seriesSet);
  }
}
