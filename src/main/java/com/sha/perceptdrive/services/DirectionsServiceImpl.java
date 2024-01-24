package com.sha.perceptdrive.services;

import com.sha.perceptdrive.beans.*;
import com.sha.perceptdrive.entities.HistoricalDriveTime;
import com.sha.perceptdrive.entities.User;
import com.sha.perceptdrive.repository.HistoricalDriveTimeRepository;
import com.sha.perceptdrive.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DirectionsServiceImpl implements DirectionsService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String GOOGLE_MAPS_API_URL = "https://maps.googleapis.com/maps/api/directions/json?origin={origin}&destination={destination}&departure_time=now&key={API_TOKEN}";
    private static final String GOOGLE_MAPS_URL = "https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s&travelmode=driving";
    
    private static final String DEFAULT_PIN = "0000";
    private static final String NOT_FOUND_STATUS = "NOT_FOUND";

    private final Client client;
    private final HistoricalDriveTimeRepository driveTimeRepo;
    private final UserRepository userRepo;

    public DirectionsServiceImpl(final Client client, HistoricalDriveTimeRepository driveTimeRepo, UserRepository userRepo) {
        this.client = client;
        this.driveTimeRepo = driveTimeRepo;
        this.userRepo = userRepo;
    }

    @Override
    public MapsResponse getMapsDirections(final String fromNumber, final String origin, final String destination) {
        MapsResponse mapsResponse;

        WebTarget target = client.target(GOOGLE_MAPS_API_URL)
                .resolveTemplate("origin", origin)
                .resolveTemplate("destination", destination)
                .resolveTemplate("API_TOKEN", API_TOKEN);
        try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
            mapsResponse = response.readEntity(MapsResponse.class);
        } catch (HttpServerErrorException e) {
            throw new RuntimeException(e);
        }
        //now that we have the maps response, we need to parse it to get eta, travel time, distance, and traffic info
        if (mapsResponse.getStatus().equalsIgnoreCase(NOT_FOUND_STATUS)) {
            return null;
        }
        saveHistDriveTime(fromNumber, origin, destination, mapsResponse);
        saveUser(fromNumber);
        return mapsResponse;
    }

    @Override
    public String getGoogleMapUrl(final String origin, final String destination) {
        return String.format(GOOGLE_MAPS_URL, origin, destination);
    }

    private void saveHistDriveTime(final String phone, final String origin, final String destination, final MapsResponse mapsResponse) {
        // Save each query that the user makes
        ZonedDateTime currentUTC = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        String trafficTime = mapsResponse.getRoutes()[0].getLegs()[0].getDurationInTraffic().getText();
        HistoricalDriveTime data = new HistoricalDriveTime();
        data.setDestination(destination);
        data.setOrigin(origin);
        data.setUserPhone(phone);
        data.setTravelTimeTraffic(trafficTimeToMin(trafficTime));
        data.setDate(Timestamp.valueOf(currentUTC.toLocalDateTime()));
        driveTimeRepo.save(data);
    }

    private void saveUser(final String phone) {
        // First we have to see if this user is unique before adding them
        List<User> user = userRepo.findUserByPhone(phone);
        if (user == null || user.isEmpty()) {
            LOG.info("Adding user with number {}", phone);
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setPin(DEFAULT_PIN);
            userRepo.save(newUser);

        } else {
            LOG.info("User already exists in the database");
        }
    }

    private int trafficTimeToMin(String trafficTime) {
        /*
         * Input may be
         * a) 3 hours 14 mins
         * b) 8 mins
         */
        // 1) parse the traffic time to an array
        // we may have [3,hours,14,mins]
        // or may have [14,mins]
        if (trafficTime.contains(" hours")) {
            int hrs = 0;
            int mins = 0;
            // we extract the hours and mins from the String, like "3 hours 26 mins"
            Pattern extractTimePatt = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
            Matcher matchTime = extractTimePatt.matcher(trafficTime);
            if (matchTime.find()) {
                hrs = Integer.parseInt(matchTime.group(0));
            }
            if (matchTime.find()) {
                mins = Integer.parseInt(matchTime.group(0));
            }
            return hrs * 60 + mins;
        }
        return Integer.parseInt(trafficTime.replace(" mins", ""));
    }
}
