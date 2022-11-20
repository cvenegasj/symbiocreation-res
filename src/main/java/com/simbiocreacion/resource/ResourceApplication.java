package com.simbiocreacion.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class ResourceApplication {

	@Value("${aws.accessKeyId}")
	private String awsAccessKeyId;

	@Value("${aws.secretAccessKey}")
	private String awsSecretAccessKey;

	public static void main(String[] args) {
		SpringApplication.run(ResourceApplication.class, args);
	}

	@Bean
	public AwsBasicCredentials awsBasicCredentials() {
		return AwsBasicCredentials.create(this.awsAccessKeyId, this.awsSecretAccessKey);
	}
}
