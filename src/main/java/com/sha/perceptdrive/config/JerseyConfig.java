package com.sha.perceptdrive.config;

import com.sha.perceptdrive.mapper.JacksonObjectMapperProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {
    private static final String PROVIDER_PACKAGES = "com.sha.perceptdrive.controllers";

    public JerseyConfig() {
        register(JacksonJsonProvider.class);
        register(JacksonObjectMapperProvider.class);
    }

    @Bean
    public ServletRegistrationBean<ServletContainer> v2Servlet() throws ClassNotFoundException {
        ServletRegistrationBean<ServletContainer> registration = new ServletRegistrationBean<>(
                new ServletContainer(new JerseyResourceConfig(PROVIDER_PACKAGES)), "/api/v1/*");
        registration.addInitParameter(ServerProperties.PROVIDER_PACKAGES, PROVIDER_PACKAGES);
        registration.addInitParameter(ServerProperties.PROVIDER_SCANNING_RECURSIVE, "false");
        registration.setName("servlets");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
