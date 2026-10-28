package com.rahul.HostelOs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HostelOsApplication {

	public static void main(String[] args) {
		SpringApplication.run(HostelOsApplication.class, args);
	}

}
