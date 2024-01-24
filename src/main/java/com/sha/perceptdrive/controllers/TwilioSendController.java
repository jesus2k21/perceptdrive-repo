package com.sha.perceptdrive.controllers;

import com.sha.perceptdrive.beans.RouteInformation;
import com.sha.perceptdrive.beans.TwilioMessageResponse;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TwilioSendController {
    /*
     * Handles all the Twilio calls to send/receive SMS messages.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ACCOUNT_SID = "ACdd798cbc037a486dc7ca0ff93ade4a2a";
    private static final String TWILIO_NUMBER = "+13093003058";
    private static final int STD_DEVIATION_THRESHOLD = 2;

    public TwilioSendController() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public TwilioMessageResponse sendRouteInfoSms(final String recipientNumber, final RouteInformation routeInformation, final double[] routeAvgsModes, final String mapsUrl, final String origin, final String destination) {
        String messageBody = buildMessageBody(routeInformation, routeAvgsModes, mapsUrl, origin, destination);
        return getTwilioMessageResponse(recipientNumber, messageBody);
    }

    public TwilioMessageResponse sendSms(final String recipientNumber, final String msgBody) {
        return getTwilioMessageResponse(recipientNumber, msgBody);
    }

    private TwilioMessageResponse getTwilioMessageResponse(String recipientNumber, String msgBody) {
        Message message = Message.creator(new PhoneNumber(recipientNumber),
                        new PhoneNumber(TWILIO_NUMBER),
                        msgBody)
                        .create();

        TwilioMessageResponse twilioMessageResponse = new TwilioMessageResponse();
        twilioMessageResponse.setBody(message.getBody());
        twilioMessageResponse.setDate_created(message.getDateCreated().toString());
        twilioMessageResponse.setDirection(String.valueOf(message.getDirection()));
        twilioMessageResponse.setError_message(message.getErrorMessage());
        twilioMessageResponse.setFrom(message.getFrom().toString());
        twilioMessageResponse.setNum_media(String.valueOf(message.getNumMedia()));
        twilioMessageResponse.setNum_segments(String.valueOf(message.getNumSegments()));
        twilioMessageResponse.setPrice(String.valueOf(message.getPrice()));
        twilioMessageResponse.setPrice_unit(String.valueOf(message.getPriceUnit()));
        twilioMessageResponse.setTo(message.getTo());
        twilioMessageResponse.setUri(message.getUri());

        return twilioMessageResponse;
    }

    private String buildMessageBody(final RouteInformation routeInformation, final double[] routeAvgsModes, final String mapsUrl, final String origin, final String destination) {

        return "Hey there! " +
                outlierMessage(routeInformation.getTraffic(), routeAvgsModes) +
                " to get to " +
                destination +
                " from " +
                origin +
                ". Distance to travel is " +
                routeInformation.getDistance() +
                ".\nTap on this Google Maps link to view your route: " +
                mapsUrl;
    }

    private String outlierMessage(final String travelTime, final double[] routeAvgsModes) {
        // We'll compare the travel time and the avg to see if it's within 2 std deviations
        // routeAvgsModes = {mean, mode, stdDeviation}
        double stdDeviations = STD_DEVIATION_THRESHOLD * routeAvgsModes[2];
        double mean = routeAvgsModes[0];
        double lowerThreshold = mean - stdDeviations;
        double upperThreshold = mean + stdDeviations;
        int time = extractTime(travelTime);
        LOG.info("Time: {} | stdDeviation: {} | lowerThreshold: {} | upperThreshold: {}", time, stdDeviations, lowerThreshold, upperThreshold);
        if (upperThreshold == 0 || lowerThreshold == 0 || lowerThreshold > upperThreshold) {
            LOG.info("Could not use historical data to determine outlier");
            return "With current traffic, it'll take about " + travelTime;
        }
        else if (time >= upperThreshold) {
            // we'll consider the travelTime irregular, but if the upperThreshold is 0,
            // then that means that there was no historical data to calculate with
            return "Looks like there's traffic up ahead. It'll take about " + travelTime + " mins";
        }
        else if (time <= lowerThreshold) {
            // we'll consider the travelTime irregular, but if the lowerThreshold is 0,
            // then that means that there was no historical data to calculate with
            return "Traffics been moving fast! It'll take about " + travelTime + " mins";
        } else {
            return "With current traffic, it'll take about " + travelTime;
        }
    }

    private int extractTime(final String travelTime) {
        if (travelTime.contains(" hours")) {
            int hrs = 0;
            int mins = 0;
            // we extract the hours and mins from the String, like "3 hours 26 mins"
            Pattern extractTimePatt = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
            Matcher matchTime = extractTimePatt.matcher(travelTime);
            if (matchTime.find()) {
                hrs = Integer.parseInt(matchTime.group(0));
            }
            if (matchTime.find()) {
                mins = Integer.parseInt(matchTime.group(0));
            }
            return hrs * 60 + mins;
        }
        return Integer.parseInt(travelTime.replace(" mins", ""));
    }

}
