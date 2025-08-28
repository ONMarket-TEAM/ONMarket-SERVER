package com.onmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OnMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnMarketApplication.class, args);
	}

}
