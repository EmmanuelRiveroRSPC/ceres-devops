package com.rackspace.rollupjobs.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RollupJobsApplication {
	public static void main(String[] args) {
		SpringApplication.run(RollupJobsApplication.class);
	}
}
