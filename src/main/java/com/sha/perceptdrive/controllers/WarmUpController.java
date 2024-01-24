package com.sha.perceptdrive.controllers;

import com.sha.perceptdrive.beans.PingResponse;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Component
public class WarmUpController {
    /*
     * this warmup controller is called whenever App Engine is
     * spinning up more instances. Additional logic can be
     * put here to do some more work during initial setup
     * if need be
     */

    private static final String DEFAULT_STATUS = "WarmedUp";

    @Path("/_ah/warmup")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public PingResponse getPing() {
        return new PingResponse(DEFAULT_STATUS);
    }
}
