package com.sha.perceptdrive;

import com.sha.perceptdrive.config.SpringApplicationConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PerceptdriveApplication {

	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder()
				.sources(SpringApplicationConfiguration.class)
				.logStartupInfo(true)
				.build()
				.run(args);
	}
}