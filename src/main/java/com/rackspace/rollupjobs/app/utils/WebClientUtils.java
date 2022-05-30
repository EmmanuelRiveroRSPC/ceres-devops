package com.rackspace.rollupjobs.app.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.rollupjobs.app.config.Properties;
import com.rackspace.rollupjobs.app.model.Job;
import com.rackspace.rollupjobs.app.model.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
public class WebClientUtils {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String URI;

    public WebClientUtils(Properties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.URI = String.format("http://%s:8080/api/job/replicate", properties.getClusterIp());
    }

    public String replicateClaimJob(Job job) {
        String result = this.restTemplate.postForObject(this.URI, getRequest(job), String.class);
        log.info("Result from replicate claim job: {}", result);
        return result;
    }

    public void replicateFreeJob(Job job) {
        this.restTemplate.put(this.URI, getRequest(job));
    }

    private HttpEntity<String> getRequest(Job job) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = null;
        try {
            request = new HttpEntity<>(objectMapper.writeValueAsString(job), headers);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return request;
    }
}
