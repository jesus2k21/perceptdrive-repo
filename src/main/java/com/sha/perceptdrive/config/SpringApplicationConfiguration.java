package com.sha.perceptdrive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.sha.perceptdrive.jackson.ObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;

@Configuration
@ComponentScan("com.sha.perceptdrive")
public class SpringApplicationConfiguration {

    @Bean
    public Client getClient() {
        return ClientBuilder.newBuilder()
                .register(JacksonJsonProvider.class)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapperFactory().create();
    }
}
