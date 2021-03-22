/*
 * Copyright 2021 Rackspace US, Inc.
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

package com.rackspace.ceres.app.web;

import static com.rackspace.ceres.app.web.TagListConverter.convertPairsListToMap;

import com.rackspace.ceres.app.config.DownsampleProperties;
import com.rackspace.ceres.app.downsample.Aggregator;
import com.rackspace.ceres.app.model.QueryResult;
import com.rackspace.ceres.app.services.QueryService;
import com.rackspace.ceres.app.utils.DateTimeUtils;
import com.rackspace.ceres.app.validator.QueryRequestValidator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Native Ceres query API endpoints.
 */
@RestController
@RequestMapping("/api/query")
@Profile("query")
public class QueryController {

  private final QueryService queryService;
  private final DownsampleProperties downsampleProperties;
  private final QueryRequestValidator queryRequestValidator;

  @Autowired
  public QueryController(QueryService queryService, DownsampleProperties downsampleProperties,
                          QueryRequestValidator queryRequestValidator) {
    this.queryService = queryService;
    this.downsampleProperties = downsampleProperties;
    this.queryRequestValidator = queryRequestValidator;
  }

  @GetMapping
  public Flux<QueryResult> query(@RequestParam(name = "tenant") String tenantParam,
      @RequestParam(required = false) String metricName,
      @RequestParam(required = false) String metricGroup,
      @RequestParam(defaultValue = "raw") Aggregator aggregator,
      @RequestParam(required = false) Duration granularity,
      @RequestParam List<String> tag,
      @RequestParam String start,
      @RequestParam(required = false) String end) {
    queryRequestValidator.validateMetricNameAndMetricGroup(metricName, metricGroup);

    Instant startTime = DateTimeUtils.parseInstant(start);
    Instant endTime = DateTimeUtils.parseInstant(end);

    if (aggregator == null || Objects.equals(aggregator, Aggregator.raw)) {
        return queryService.queryRaw(tenantParam, metricName, metricGroup,
            convertPairsListToMap(tag),
            startTime, endTime
        );
    } else {
      if (granularity == null) {
        granularity = DateTimeUtils
            .getGranularity(startTime, endTime, downsampleProperties.getGranularities());
      }
      return queryService.queryDownsampled(tenantParam, metricName, metricGroup,
          aggregator,
          granularity,
          convertPairsListToMap(tag),
          startTime, endTime
      );
    }
  }
}
