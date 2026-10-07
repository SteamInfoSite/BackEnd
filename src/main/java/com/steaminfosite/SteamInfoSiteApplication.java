package com.steaminfosite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SteamInfoSiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(SteamInfoSiteApplication.class, args);
	}

}
