package com.sha.perceptdrive.config;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

public class JerseyResourceConfig extends ResourceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final List<Class<? extends Annotation>> ANNOTATIONS = Arrays.asList(Path.class, Provider.class);

    public JerseyResourceConfig(final String packages) throws ClassNotFoundException {
        String[] packagesArr = packages.split(";");

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        for (Class<? extends Annotation> annotation : ANNOTATIONS) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));
        }

        for (String packageName : packagesArr) {
            LOG.info("Registering package resources with Jersey: {}", packageName);
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(packageName)) {
                register(ClassUtils.forName(Objects.requireNonNull(beanDefinition.getBeanClassName()), getClassLoader()));
            }
        }
        register(JacksonJsonProvider.class);
    }
}