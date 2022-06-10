package com.rackspace.mongobuffer.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("buffer")
@Component
@Data
@Validated
public class Properties {
    /**
     * Specifies max number of entries before flushing to mongodb
     */
    int maxNum;
}
