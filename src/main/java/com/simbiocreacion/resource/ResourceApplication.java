package com.simbiocreacion.resource;

import com.simbiocreacion.resource.model.OneDot;
import com.simbiocreacion.resource.model.Symbiocreation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxProcessor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

@SpringBootApplication
@EnableScheduling
public class ResourceApplication {

	@Value("${aws.accessKeyId}")
	private String awsAccessKeyId;

	@Value("${aws.secretAccessKey}")
	private String awsSecretAccessKey;

	public static void main(String[] args) {
		SpringApplication.run(ResourceApplication.class, args);
	}

	@Bean
	public FluxProcessor symbioProcessor() {
		final FluxProcessor<Symbiocreation, Symbiocreation> processor = DirectProcessor.<Symbiocreation>create().serialize();
		return processor;
	}

//	@Bean
//	public Sinks.Many oneDotSink() {
//		final Sinks.Many<OneDot> sink = Sinks.many().multicast().onBackpressureBuffer();
//		return sink;
//	}

	@Bean
	public FluxProcessor oneDotProcessor() {
		final FluxProcessor<OneDot, OneDot> processor = DirectProcessor.<OneDot>create().serialize();
		return processor;
	}

	@Bean
	public AwsBasicCredentials awsBasicCredentials() {
		return AwsBasicCredentials.create(this.awsAccessKeyId, this.awsSecretAccessKey);
	}
}
