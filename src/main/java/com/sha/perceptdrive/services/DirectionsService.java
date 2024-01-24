package com.sha.perceptdrive.services;

import com.sha.perceptdrive.beans.MapsResponse;

public interface DirectionsService {
    MapsResponse getMapsDirections(String fromNumber, String origin, String destination);

    String getGoogleMapUrl(String origin, String destination);
}
