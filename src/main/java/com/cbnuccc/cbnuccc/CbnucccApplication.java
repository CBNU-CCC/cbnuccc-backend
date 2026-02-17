package com.cbnuccc.cbnuccc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CbnucccApplication {

	public static void main(String[] args) {
		SpringApplication.run(CbnucccApplication.class, args);
	}

}
