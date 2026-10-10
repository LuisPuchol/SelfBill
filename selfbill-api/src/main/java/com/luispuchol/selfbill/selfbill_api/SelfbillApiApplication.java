package com.luispuchol.selfbill.selfbill_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SelfbillApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SelfbillApiApplication.class, args);
	}

}
