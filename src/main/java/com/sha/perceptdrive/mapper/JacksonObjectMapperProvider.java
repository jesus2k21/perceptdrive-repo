package com.sha.perceptdrive.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sha.perceptdrive.jackson.ObjectMapperFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    public JacksonObjectMapperProvider() {
        mapper = new ObjectMapperFactory().create();
    }

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return mapper;
    }
}
