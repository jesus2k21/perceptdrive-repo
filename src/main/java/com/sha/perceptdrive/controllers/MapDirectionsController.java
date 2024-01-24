package com.sha.perceptdrive.controllers;

import com.sha.perceptdrive.services.SMSBodyProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sha.perceptdrive.beans.MapsResponse;
import com.sha.perceptdrive.beans.RouteInformation;
import com.sha.perceptdrive.beans.RouteOriDest;
import com.sha.perceptdrive.beans.TwilioMessageResponse;
import com.sha.perceptdrive.entities.HistoricalDriveTime;
import com.sha.perceptdrive.repository.HistoricalDriveTimeRepository;
import com.sha.perceptdrive.services.DirectionsService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Path("/directions")
@Component
public class MapDirectionsController {
    /*
     * Controller class gets the map direction provided by the client, sends it to the service layer, and returns the response.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final DirectionsService directionsService;
    private final SMSBodyProcessorService smsProcessor;
    private final TwilioSendController twilioController;
    private final HistoricalDriveTimeRepository driveTimeRepository;

    public MapDirectionsController(final DirectionsService directionsService, final SMSBodyProcessorService smsProcessor,
                                   final TwilioSendController twilioController,
                                   final HistoricalDriveTimeRepository driveTimeRepository) {
        this.directionsService = directionsService;
        this.smsProcessor = smsProcessor;
        this.twilioController = twilioController;
        this.driveTimeRepository = driveTimeRepository;
    }

    @POST
    @Path("/route")
    @Consumes(MediaType.APPLICATION_JSON)
    public void getDirections(final RouteOriDest directions) {
        /* This is used as a tester method, needs to be kept up to date with what gets sent to the user */
        /*
         * We need to post to the database:
         * 1 - The destination
         * 2 - The origin
         * 3 - The traffic calculated time it takes to travel there
         * 4 - The date/time that the request was made
         */
        LOG.info("Attempting to get directions from {} to {}", directions.getOrigin(), directions.getDestination());
        MapsResponse mapsResponse = directionsService.getMapsDirections("+19366897391", directions.getOrigin(), directions.getDestination());
        String googleMapsUrl = directionsService.getGoogleMapUrl(directions.getOrigin(), directions.getDestination());
        LOG.info("Status for getDirections: {}", mapsResponse.getStatus());
        RouteInformation routeInformation = new RouteInformation();
        String trafficTime = mapsResponse.getRoutes()[0].getLegs()[0].getDurationInTraffic().getText();
        routeInformation.setTravelTime(mapsResponse.getRoutes()[0].getLegs()[0].getDuration().getText());
        routeInformation.setDistance(mapsResponse.getRoutes()[0].getLegs()[0].getDistance().getText());
        routeInformation.setTraffic(trafficTime);

        HistoricalDriveTime data = new HistoricalDriveTime();
        data.setDestination(directions.getDestination());
        data.setOrigin(directions.getOrigin());
        data.setUserPhone("+19366897391");
        data.setTravelTimeTraffic(trafficTimeToMin(trafficTime));
        data.setDate(Timestamp.valueOf(LocalDateTime.now()));

        driveTimeRepository.save(data);

        TwilioMessageResponse twilioResponse = twilioController.sendRouteInfoSms("+19366897391", routeInformation, new double[] {0,0}, googleMapsUrl, directions.getOrigin(), directions.getDestination());
        LOG.info("Twilio message to {} has status: {}. It cost {} to send.", twilioResponse.getTo(), twilioResponse.getStatus(), twilioResponse.getPrice());
    }

    public int trafficTimeToMin(String trafficTime) {
        /*
         * Input may be
         * a) 3 hours 14 mins - [3,hours,14,mins]
         * b) 8 mins - [14,mins]
         */
        String[] trafficTimeArray = trafficTime.split(" ");

        if (trafficTimeArray.length == 2) { // it only has minutes
            return Integer.parseInt(trafficTimeArray[0]);
        }
        else {
            // we have hrs and mins
            int hrs = Integer.parseInt(trafficTimeArray[0]);
            int mins = Integer.parseInt(trafficTimeArray[2]);
            return (hrs * 60) + mins;
        }
    }

}
