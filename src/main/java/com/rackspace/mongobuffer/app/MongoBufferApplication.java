package com.rackspace.mongobuffer.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MongoBufferApplication {
	public static void main(String[] args) {
		SpringApplication.run(MongoBufferApplication.class);
	}
}
