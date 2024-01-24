package com.sha.perceptdrive.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperFactory {

    public ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        //mapper.registerModule(new JavaTimeModule());
        // mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

}
