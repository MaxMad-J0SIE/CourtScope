package com.madmax.courtscope;

import org.springframework.boot.SpringApplication;

public class TestCourtscopeApplication {

	public static void main(String[] args) {
		SpringApplication.from(CourtscopeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
