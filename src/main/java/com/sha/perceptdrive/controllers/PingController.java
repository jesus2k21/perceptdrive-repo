package com.sha.perceptdrive.controllers;

import com.sha.perceptdrive.beans.PingResponse;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
//access while running on localhost: http://localhost:8080/api/v1/ping

@Path("/")
@Component
public class PingController {
    private static final String DEFAULT_STATUS = "Healthy";

    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public PingResponse getPing() {
        return new PingResponse(DEFAULT_STATUS);
    }
}
