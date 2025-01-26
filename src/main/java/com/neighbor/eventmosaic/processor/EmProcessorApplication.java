package com.neighbor.eventmosaic.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EmProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmProcessorApplication.class, args);
	}

}
