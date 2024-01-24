package com.sha.perceptdrive.beans;

public class PingResponse {
    private final String status;

    public PingResponse(final String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
