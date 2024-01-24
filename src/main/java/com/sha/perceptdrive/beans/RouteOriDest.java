package com.sha.perceptdrive.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteOriDest {
    /*
     * Class contains the following information:
     * - origin: the starting point of the route
     * - destination: the ending point of the route
     * This will be obtained from the user.
     */
    @NotNull
    private String origin;
    @NotNull
    private String destination;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

}
