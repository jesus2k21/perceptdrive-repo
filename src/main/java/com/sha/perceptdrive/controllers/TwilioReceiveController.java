package com.sha.perceptdrive.controllers;

import com.sha.perceptdrive.services.DriveTimeAnalysisService;
import com.sha.perceptdrive.utils.MsgUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.language.v1.Entity;
import com.sha.perceptdrive.beans.MapsResponse;
import com.sha.perceptdrive.beans.RouteInformation;
import com.sha.perceptdrive.beans.TwilioMessageResponse;
import com.sha.perceptdrive.entities.SavedLocation;
import com.sha.perceptdrive.entities.Schedule;
import com.sha.perceptdrive.entities.UserTextInput;
import com.sha.perceptdrive.repository.SavedLocationRepository;
import com.sha.perceptdrive.repository.ScheduleRepository;
import com.sha.perceptdrive.repository.UserTextInputRepository;
import com.sha.perceptdrive.services.DirectionsService;
import com.sha.perceptdrive.services.NaturalLanguageProcessor;
import com.sha.perceptdrive.services.SMSBodyProcessorService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandles;
import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/handleSMS")
@Component
public class TwilioReceiveController {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TRAFFIC_NOW = "traffic_now";
    private static final String SAVE = "save";
    private static final String SCHEDULE = "schedule";
    private static final String PAUSE = "pause";

    private final SMSBodyProcessorService smsProcessor;
    private final DirectionsService directionsService;
    private final TwilioSendController twilioController;
    private final NaturalLanguageProcessor languageProcessor;
    private final SavedLocationRepository savedLocationRepo;
    private final ScheduleRepository scheduleRepo;
    private final UserTextInputRepository userTextRepo;
    private final DriveTimeAnalysisService timeAnalysisService;

    public TwilioReceiveController(final SMSBodyProcessorService smsProcessor, final DirectionsService directionsService,
                                   final TwilioSendController twilioController, final SavedLocationRepository savedLocationRepo,
                                   final ScheduleRepository scheduleRepo, final UserTextInputRepository userTextRepo,
                                   final NaturalLanguageProcessor languageProcessor, final DriveTimeAnalysisService timeAnalysisService) {
        this.smsProcessor = smsProcessor;
        this.directionsService = directionsService;
        this.twilioController = twilioController;
        this.savedLocationRepo = savedLocationRepo;
        this.scheduleRepo = scheduleRepo;
        this.userTextRepo = userTextRepo;
        this.languageProcessor = languageProcessor;
        this.timeAnalysisService = timeAnalysisService;
    }

    @POST
    @Produces(MediaType.APPLICATION_XML)
    public void receiveSMS(@RequestParam("Body") String fullSMSResponse) {
        /*
         * We now have to filter instances where the user wants to save destinations or schedule times
         */

        /*
        SMS Response
        ToCountry=US&ToState=IL&SmsMessageSid=SM3b933f61f63f4c38225ea5e52f1b886a&NumMedia=0&ToCity=BLOOMINGTON&FromZip=77371&SmsSid=SM3b933f61f63f4c38225ea5e52f1b886a&FromState=TX&SmsStatus=received&FromCity=CONROE&Body=I%27m+going+to+resi+from+apartment&FromCountry=US&To=%2B13093003058&ToZip=61761&NumSegments=1&MessageSid=SM3b933f61f63f4c38225ea5e52f1b886a&AccountSid=ACdd798cbc037a486dc7ca0ff93ade4a2a&From=%2B19366897391&ApiVersion=2010-04-01
        MMS Response
        ToCountry=US&MediaContentType0=text%2Fx-vcard&ToState=IL&SmsMessageSid=MMc59e0eb6a251eac53e6fc6dcb98187d9&NumMedia=1&ToCity=BLOOMINGTON&FromZip=77371&SmsSid=MMc59e0eb6a251eac53e6fc6dcb98187d9&FromState=TX&SmsStatus=received&FromCity=CONROE&Body=&FromCountry=US&To=%2B13093003058&ToZip=61761&NumSegments=1&MessageSid=MMc59e0eb6a251eac53e6fc6dcb98187d9&AccountSid=ACdd798cbc037a486dc7ca0ff93ade4a2a&From=%2B19366897391&MediaUrl0=https%3A%2F%2Fapi.twilio.com%2F2010-04-01%2FAccounts%2FACdd798cbc037a486dc7ca0ff93ade4a2a%2FMessages%2FMMc59e0eb6a251eac53e6fc6dcb98187d9%2FMedia%2FME36f9283b0ea6df8d1cc214c2b4ae2587&ApiVersion=2010-04-01
         */
        String fromNumber = smsProcessor.formatFromNumber(fullSMSResponse);
        if (StringUtils.isEmpty(fromNumber)) {
            LOG.error("Could not get the from number. This is a bug.");
            return;
        }
        LOG.info("Processing full SMS response from {}", fromNumber);
        String msgBody;
        try {
            msgBody = fullSMSResponse.substring(fullSMSResponse.indexOf("Body=")+5, fullSMSResponse.indexOf("&FromCountry"));
        } catch (StringIndexOutOfBoundsException e) {
            LOG.warn("User either reacted to the text or sent nothing");
            twilioController.sendSms("+" + fromNumber, "If you want to find traffic time between two addresses, format your request like \"I'm going to 350 E Stacy Rd Allen TX from Trail Ridge Rd Grand Lake CO\". You can also save a location like your school address like \"Save 400 Bizzell St College Station TX as school\". Lastly, you can schedule traffic updates via text with a request like \"Schedule updates from apartment to school on weekdays at 8 am\".");
            return;
        }
        LOG.info("User {} request: {}", fromNumber, msgBody);
        /*
         * msgBody can now have 2 possibilities:
         * 1 - Im going to {301 N Greenville Ave Allen TX} from {3409 N Central Expressway Plano TX}
         * 2 - {Save} {301 N Greenville Ave Allen TX} as {apartment}
         * 3 - {Schedule} texts to send {weekdays} at {8 am} {and} {5 pm}
         */
        // Create custom ML model to be able to handle getting the needed entities
        // List<Entity> entities = languageProcessor.analyzeEntities(smsProcessor.removePlus(msgBody));
        // int operation = getOperationStrategy(entities);

        if (msgBody.toLowerCase().contains("from") && !msgBody.toLowerCase().contains("schedule")) {
            // from is a unique word that users should use
            String[] userOriginDest = smsProcessor.formatOriDest(msgBody);
            if (userOriginDest == null) {
                LOG.warn("Something went wrong in the processing.");
                twilioController.sendSms("+" + fromNumber, "Oops! Looks like I can't find that address. Please try adjusting them. You can send your request like \"To work from school\"");
                return;
            }
            // save the users text as traffic_now
            UserTextInput input = new UserTextInput();
            input.setUserTextInput(smsProcessor.replaceCharacters(msgBody));
            input.setTextType(TRAFFIC_NOW);
            userTextRepo.save(input);

            String origin = smsProcessor.replaceCharacters(userOriginDest[0].toLowerCase().trim());
            String destination = userOriginDest[1].toLowerCase().trim();

            // if we receive two values, then we call google api to return directions
            LOG.info("Origin: {}", userOriginDest[0]);
            LOG.info("Destination: {}", userOriginDest[1]);

            // Check to see if the user provided a saved location in their origin/destination
            SavedLocation originLocation = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, origin);
            SavedLocation destLocation = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, destination);

            if (originLocation != null) {
                origin = originLocation.getLocation();
            }
            if (destLocation != null) {
                destination = destLocation.getLocation();
            }

            MapsResponse mapsResponse = directionsService.getMapsDirections(fromNumber, origin, destination);
            if (mapsResponse == null) {
                LOG.warn("Could not find an address for origin: {} and destination: {}", origin, destination);
                twilioController.sendSms("+" + fromNumber, "Oops! Looks like I can't find that address. Please try adjusting them.");
                return;
            }
            String googleMapsUrl = directionsService.getGoogleMapUrl(origin, destination);
            LOG.info("Google maps URL for user {} - to {} from {}: {}", fromNumber, destination, origin, googleMapsUrl);

            String trafficTime = mapsResponse.getRoutes()[0].getLegs()[0].getDurationInTraffic().getText();

            RouteInformation routeInformation = new RouteInformation();
            routeInformation.setTravelTime(mapsResponse.getRoutes()[0].getLegs()[0].getDuration().getText());
            routeInformation.setDistance(mapsResponse.getRoutes()[0].getLegs()[0].getDistance().getText());
            routeInformation.setTraffic(trafficTime);

            ZonedDateTime currentUTC = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
            double[] meanModeStdDevTimes = timeAnalysisService.getMeanModeDriveTime(currentUTC, origin, destination);

            TwilioMessageResponse twilioResponse = twilioController.sendRouteInfoSms("+" + fromNumber, routeInformation, meanModeStdDevTimes, googleMapsUrl, smsProcessor.replaceCharacters(userOriginDest[0]), smsProcessor.replaceCharacters(userOriginDest[1]));
            LOG.info("Twilio message to {} has status: {}. It cost {} to send.", twilioResponse.getTo(), twilioResponse.getStatus(), twilioResponse.getPrice());
        }
        else if (msgBody.toLowerCase().contains("save")) {
            // the save keyword is unique for this case
            // 2 - {Save} {301 N Greenville Ave Allen TX} as {apartment}
            // save the users text for training
            UserTextInput input = new UserTextInput();
            input.setUserTextInput(smsProcessor.replaceCharacters(msgBody));
            input.setTextType(SAVE);
            userTextRepo.save(input);

            String[] parsedSave = smsProcessor.formatSaveDest(msgBody);
            if (parsedSave == null) {
                LOG.warn("Could not parse the users Save request: {}", smsProcessor.replaceCharacters(msgBody));
                twilioController.sendSms("+" + fromNumber, "Hmm, I couldn't quite understand that. Try formatting your request like 'Save Trail Ridge Rd Grand Lake CO as school'");
                return;
            }
            SavedLocation location = new SavedLocation();
            location.setLocation(parsedSave[0]);
            location.setNickname(parsedSave[1]);
            location.setPhone(fromNumber);
            // First check to see if this location already exists
            SavedLocation locationExistence = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, parsedSave[1]);
            if (locationExistence != null) {
                LOG.warn("User {} tried to save a location with the same name {}", fromNumber, parsedSave[1]);
                twilioController.sendSms("+" + fromNumber, "Oops! Looks like you already have a location saved with the name " + parsedSave[1]);
                return;
            }
            savedLocationRepo.save(location);
            TwilioMessageResponse twilioResponse = twilioController.sendSms("+" + fromNumber, "Successfully saved " + smsProcessor.replaceCharacters(parsedSave[0]) + " as "+ parsedSave[1]);
            LOG.info("Twilio message to {} has status: {}. It cost {} to send.", twilioResponse.getTo(), twilioResponse.getStatus(), twilioResponse.getPrice());
        }
        else if (msgBody.toLowerCase().contains("schedule") && !msgBody.toLowerCase().contains("delete") && !msgBody.toLowerCase().contains("list")) {
            // the schedule keyword is also unique to this
            // 3 - {Schedule} updates to send from {apartment} to {work} on {weekdays} at {8 am}
            // save the users text for training
            UserTextInput input = new UserTextInput();
            input.setUserTextInput(smsProcessor.replaceCharacters(msgBody));
            input.setTextType(SCHEDULE);
            userTextRepo.save(input);

            String[] parsedSchedule = smsProcessor.formatSchedule(msgBody);
            if (parsedSchedule == null) {
                LOG.warn("Could not parse the users Schedule request: {}", msgBody);
                twilioController.sendSms("+" + fromNumber, "Hmm, I couldn't understand that. Try formatting your request like \"Schedule updates from apartment to resi on weekdays at 5pm\"");
                return;
            }
            SavedLocation origin = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, parsedSchedule[0]);
            if (origin == null) {
                LOG.warn("Could not find saved location with nickname {}", parsedSchedule[0]);
                twilioController.sendSms("+" + fromNumber, "Hmm, it looks like you don't have a saved location with that nickname. To save a location, message \"Save 400 Bizzell St College Station TX as school\"");
                return;
            }
            if (!parsedSchedule[2].toLowerCase().contains("weekdays") && !parsedSchedule[2].toLowerCase().contains("weekends")) {
                LOG.info("User tried scheduling on a day that's not weekends or weekdays: {}", parsedSchedule[2]);
                twilioController.sendSms("+" + fromNumber, "Looks like you're wanting to schedule on " + parsedSchedule[2] + " but that isn't supported yet:( Perceptdrive currently supports \"weekends\" or \"weekdays\"");
                return;
            }

            Time time = getSqlTime(parsedSchedule[3]);
            if (time == null) {
                LOG.info("User {} tried scheduling a time for a non-multiple of 5 minute", fromNumber);
                twilioController.sendSms("+" + fromNumber, "You'll only be able to schedule times at a multiple of 5. Please consider a different time.");
                return;
            }
            Schedule schedule = new Schedule();
            schedule.setOrigin(parsedSchedule[0]);
            schedule.setDestination(parsedSchedule[1]);
            schedule.setDays(parsedSchedule[2]);
            schedule.setPhone(fromNumber);
            schedule.setTime(time);
            schedule.setSavedLocation(origin);
            // Check to see if a schedule like this already exists
            // We want the origin, destination, and time to be a unique set
            List<Schedule> schedulesExistence = scheduleRepo.findAllByPhoneAndTime(fromNumber, time);
            boolean scheduleExists = processSchedules(schedulesExistence, schedule);
            if (scheduleExists) {
                LOG.warn("Cannot save users schedule as it already exists - Origin: {}, Destination: {}, Time: {}", parsedSchedule[0], parsedSchedule[1], time);
                twilioController.sendSms("+" + fromNumber, "Oops! I can't save that schedule since you already have a schedule for that origin/destination/time combo. Consider a different time.");
                return;
            }
            scheduleRepo.save(schedule);
            TwilioMessageResponse twilioResponse = twilioController.sendSms("+" + fromNumber, "Okay! I'll send you texts at " + parsedSchedule[3].replace("+", " ") + " on " + parsedSchedule[2].replace("+", " ") + " regarding how long it will take to get to " + parsedSchedule[1].replace("+", " ") + " from " + parsedSchedule[0].replace("+", " "));
            LOG.info("Twilio message to {} has status: {}. It cost {} to send.", twilioResponse.getTo(), twilioResponse.getStatus(), twilioResponse.getPrice());
        }
        else if (msgBody.toLowerCase().contains("help")) {
            twilioController.sendSms("+" + fromNumber, "Hey there! If you want to find traffic time between two addresses, format your request like \"I'm going to 350 E Stacy Rd Allen TX from Trail Ridge Rd Grand Lake CO\". You can also save a location like your school address like \"Save 400 Bizzell St College Station TX as school\". Lastly, you can schedule traffic updates via text with a request like \"Schedule updates from apartment to school on weekdays at 8 am\".");
        }
        else if (msgBody.toLowerCase().contains("change")) {
            // {Change} {school} address to 2423 Blinn Blvd, Bryan, TX
            // We'll assume the second element in this string is what they want to change
            // get the saved location name & get the address they want to change to
            String[] destNickNameNewAddress = smsProcessor.formatChangeSavedDest(msgBody);
            if (destNickNameNewAddress == null) {
                String msg = "Oops! I couldn't understand that. Try formatting your change request like \"Change school address to 2423 Blinn Blvd, Bryan, TX\"";
                twilioController.sendSms("+" + fromNumber, msg);
                return;
            }
            SavedLocation location = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, destNickNameNewAddress[0]);
            location.setLocation(destNickNameNewAddress[1]);
            savedLocationRepo.save(location);
            LOG.info("Successfully updated the address of {} with nickname {}", fromNumber, destNickNameNewAddress[0]);
            twilioController.sendSms("+" + fromNumber, "Successfully changed the address of " + destNickNameNewAddress[0] + " to " + smsProcessor.replaceCharacters(destNickNameNewAddress[1]));
        }
        else if (msgBody.toLowerCase().contains("rename")) {
            // {Rename} {school} location to job
            String[] destRenameSavedDest = smsProcessor.formatRenameSavedDest(msgBody);
            if (destRenameSavedDest == null) {
                String msg = "Oops! I couldn't understand that. Try formatting your rename request like \"Rename job location to resi\"";
                twilioController.sendSms("+" + fromNumber, msg);
                return;
            }
            SavedLocation location = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, destRenameSavedDest[0]);
            location.setNickname(destRenameSavedDest[1]);
            savedLocationRepo.save(location);
            LOG.info("Successfully renamed the nickname of location for user {}: {} to {}", fromNumber, destRenameSavedDest[0], destRenameSavedDest[1]);
            twilioController.sendSms("+" + fromNumber, "Successfully renamed the nickname of your location " + smsProcessor.replaceCharacters(destRenameSavedDest[0]) + " to " + smsProcessor.replaceCharacters(destRenameSavedDest[1]));

        }
        else if (msgBody.toLowerCase().contains("delete")) {
            // Delete either their saved location (by name) or a schedule (by time)
            // Delete {school} - to delete their saved location
            // Delete {schedule} from {apartment} to {work} at {8:45 am}
            String[] objToDelete = smsProcessor.formatDeleteSavedDestSchedule(msgBody);
            if (objToDelete == null) {
                LOG.warn("Could not process delete request for user {}", fromNumber);
                String message = "Ooops! Looks like I couldn't quite process your delete request. If you're wanting to delete your saved location, try saying \"Delete resi\". If you're wanting to delete a schedule, try saying \"Delete schedule from apartment to resi at 8:00 am\"";
                twilioController.sendSms("+" + fromNumber, message);
                return;
            }
            if (objToDelete.length == 1) {
                // User is wanting to delete their saved location
                SavedLocation location = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(fromNumber, objToDelete[0]);
                if (location == null) {
                    LOG.info("User {} tried to delete a non-existent saved location", fromNumber);
                    String message = "Looks like there is no saved location with the nickname " + objToDelete[0] + ". Double check its name with your saved locations by messaging \"list\"";
                    twilioController.sendSms("+" + fromNumber, message);
                    return;
                }
                savedLocationRepo.delete(location);
                LOG.info("Successfully deleted saved location {} for user {}", location.getNickname(), fromNumber);
                String message = "Successfully deleted your saved location " + location.getNickname();
                twilioController.sendSms("+" + fromNumber, message);
            }
            if (objToDelete.length == 3) {
                // User is wanting to delete their schedule
                Time time = getSqlTime(objToDelete[2]);
                Schedule schedule = scheduleRepo.findScheduleByPhoneAndTime(fromNumber, time);
                if (schedule == null) {
                    LOG.info("User {} tried to delete a non-existent schedule", fromNumber);
                    String message = "Looks like there is no saved schedule with that origin, destination, and time. Double check it's name with your saved locations by messaging \"list\"";
                    twilioController.sendSms("+" + fromNumber, message);
                    return;
                }
                // There should only be one, but we'll delete all that happen to appear
                scheduleRepo.delete(schedule);
                LOG.info("Successfully deleted a schedule for {} with origin {}, destination {}, and time {}", fromNumber, schedule.getOrigin(), schedule.getDestination(), schedule.getTime());
                String message = "Successfully deleted your schedule from " + schedule.getOrigin() + " to " + schedule.getDestination() + " at " + schedule.getTime() + " CST";
                twilioController.sendSms("+" + fromNumber, message);
            }
        }
        else if (msgBody.toLowerCase().contains("list")) {
            // List all saved locations
            // List all schedules
            List<SavedLocation> savedLocations = savedLocationRepo.findAllByPhone(fromNumber);
            List<Schedule> savedSchedules = scheduleRepo.findAllByPhone(fromNumber);
            if ((savedLocations == null || savedLocations.isEmpty()) && (savedSchedules == null || savedSchedules.isEmpty())) {
                // the user has no saved locations or saved schedules
                LOG.info("User {} tried to list their saved locations/schedules but does not have any", fromNumber);
                twilioController.sendSms("+" + fromNumber, "Looks like you don't have any saved locations or schedules! You can save a location such as your school address like \"Save 400 Bizzell St College Station TX as school\". Lastly, you can schedule traffic updates via text with a request like \"Schedule updates from apartment to school on weekdays at 8 am\"");
                return;
            }
            StringBuilder sb = new StringBuilder();
            if (savedLocations != null && !savedLocations.isEmpty()) {
                sb.append("Saved Locations:\n");
                for (SavedLocation location : savedLocations) {
                    sb.append(smsProcessor.replaceCharacters(location.getNickname())).append(": ").append(smsProcessor.replaceCharacters(location.getLocation())).append("\n");
                }
            }
            if (savedSchedules != null && !savedSchedules.isEmpty()) {
                sb.append("\nSaved Schedules:\n");
                for (Schedule sched : savedSchedules) {
                    sb.append(sched.getOrigin()).append(" to ").append(sched.getDestination()).append(" on ").append(sched.getDays()).append(" at ").append(sched.getTime()).append(" CST\n");
                }
            }
            LOG.info("Sending user {} a listing of their saved locations and schedules", fromNumber);
            twilioController.sendSms("+" + fromNumber, sb.toString());
        }
        else if (msgBody.toLowerCase().contains("pause")) {
            // If the user would like to pause notifications for a certain number of days
            // We'll save that to the database and use that value to check if we should
            // send a notification or not. We'll save the value to the database along with the start date/time
            // and we'll say its inclusive of the current day. So if you say "pause for 2 days" and it's a monday
            // Notifications will resume wednesday at midnight
            UserTextInput input = new UserTextInput();
            input.setUserTextInput(smsProcessor.replaceCharacters(msgBody));
            input.setTextType(PAUSE);
            userTextRepo.save(input);
            // Get the num of days they want to pause
            int numDaysToPause = smsProcessor.formatNumPauseDays(msgBody);
            if (numDaysToPause == 0 || numDaysToPause > 365) {
                LOG.warn("Tried to pause for an unsupported number of days: {}", numDaysToPause);
                twilioController.sendSms("+" + fromNumber, "Hmm, looks like I can't accommodate that number of days to pause. Try 1 to 365 days.");
                return;
            }
            // Save the current time and days to the DB
            // add additional columns to the schedule table: days_paused and end_pause_date
            Date endDate = Date.valueOf(LocalDate.now(ZoneId.of("America/Chicago")).plusDays(numDaysToPause));

            // Since we will be pausing all notifications for all schedules, we'll query for all Schedules and update them
            List<Schedule> schedulesToBeUpdated = scheduleRepo.findAllByPhone(fromNumber);
            if (schedulesToBeUpdated.isEmpty()) {
                LOG.warn("User {} tried to pause notifications, but they do not have any schedules", fromNumber);
                twilioController.sendSms(fromNumber, MsgUtils.MSG_PAUSE_NOTIF_FAIL);
                return;
            }
            // We received some amount of schedules, so we'll update each one
            StringBuilder successMsg = new StringBuilder();
            successMsg.append("Updated:\n");
            for (Schedule schedule : schedulesToBeUpdated) {
                schedule.setDaysPaused(numDaysToPause);
                schedule.setEndPauseDate(endDate);
                scheduleRepo.save(schedule);
                successMsg.append(schedule.getOrigin()).append(" to ").append(schedule.getDestination()).append("\n");
            }
            twilioController.sendSms(fromNumber, successMsg.toString());
        }
        else {
            LOG.warn("Could not handle text from {}, sending help menu", fromNumber);
            twilioController.sendSms("+" + fromNumber, "If you want to find traffic time between two addresses, format your request like \"I'm going to 350 E Stacy Rd Allen TX from Trail Ridge Rd Grand Lake CO\". You can also save a location like your school address like \"Save 400 Bizzell St College Station TX as school\". Lastly, you can schedule traffic updates via text with a request like \"Schedule updates from apartment to school on weekdays at 8 am\".");
        }
    }

    private boolean processSchedules(final List<Schedule> schedules, final Schedule userSchedule) {
        if (schedules == null) {
            return false;
        }
        for (Schedule sched : schedules) {
            if (sched.getDestination().equalsIgnoreCase(userSchedule.getDestination()) && sched.getOrigin().equalsIgnoreCase(userSchedule.getOrigin())
                    && sched.getTime().equals(userSchedule.getTime())) {
                return true;
            }
        }
        return false;
    }

    private int getOperationStrategy(final List<Entity> entities) {
        /*
         * Goal is to determine if we are going to
         * 0 - Just send the user a quick text for a traffic update
         * 1 - Save a location as a nickname
         * 2 - Schedule updates for them
         */
        /*
         * msgBody can now have 2 possibilities:
         * 1 - Im going to {301 N Greenville Ave Allen TX} from {3409 N Central Expressway Plano TX}
         * 2 - {Save} {301 N Greenville Ave Allen TX} as {apartment}
         * 3 - {Schedule} updates from {location1_nickname} to {location2_nickname} on {weekdays} at {8 am}
         *
         * 1 - yields 2 address entities, but had to remove the 'N' abbreviation. If two address values could not be found, consider telling the user to try removing N, S, E, W
         * 2 - yields 1 address entity, and 'apartment' has the highest salience value for LOCATION. Maybe we can determine
         * the highest salience value entity that is not address and cross reference it with what regex yields is the nickname
         * 3 - Highest salience value is "Schedule updates", there are 2 LOCATION entities, and 2 numbers (or 1) for the time
         * Suggest users to use nouns for their nicknames, not verbs/adjectives
         */
        // How many address entities are there?
        Map<String, Float> locationNameSalience = new HashMap<>();
        Map<String, Float> addressNameSalience = new HashMap<>();
        Map<String, Float> numberNameSalience = new HashMap<>();
        Map<String, Float> otherNameSalience = new HashMap<>();
        for (int k = 0; k < entities.size(); k++) {
            if (entities.get(k).getType() == Entity.Type.ADDRESS) {
                addressNameSalience.put(entities.get(k).getName(), entities.get(k).getSalience());
            }
            else if (entities.get(k).getType() == Entity.Type.LOCATION) {
                locationNameSalience.put(entities.get(k).getName(), entities.get(k).getSalience());
            }
            else if (entities.get(k).getType() == Entity.Type.NUMBER) {
                numberNameSalience.put(entities.get(k).getName(), entities.get(k).getSalience());
            }
            else if (entities.get(k).getType() == Entity.Type.OTHER) {
                otherNameSalience.put(entities.get(k).getName(), entities.get(k).getSalience());
            }
        }
        // if there are 2 address entities, we're going to assume that the user is wanting to give immediate directions from one place to another

        // if there is 1 address entity and a noun with the high salience, and it has the "Save" keyword at the beginning, then they want to save a place

        // if they provide two LOCATION with the highest saliences, and "Schedule" is the keyword at the beginning, and
        return 0;
    }

    private Time getSqlTime(final String time) {
        // something like 8am or 8 am or 12:30 pm
        // 8:00 am, 8am, 12:30 pm
        // We also have to check if it's a multiple of 5
        String[] parsedTime = time.replace("+", " ").split(" ");;
        if (parsedTime.length == 2) {
            // user put in 8:00 am or 12:30 pm or 7 am or 1 pm
            // we have {8:00, am} or {12:30, pm} or {8, am}
            String timeVal = parsedTime[0].strip();
            boolean containsColon = timeVal.contains(":");
            boolean isAm = parsedTime[1].toLowerCase().contains("am");

            if (containsColon) {
                // we'll parse at the colon
                String[] parsedTimeVal = timeVal.split(":");
                // we have {8, 00}
                int hour = Integer.parseInt(parsedTimeVal[0]);
                int min = Integer.parseInt(parsedTimeVal[1]);
                if (min % 5 != 0) {
                    return null;
                }
                if (!isAm && hour != 12) {
                    hour += 12;
                }
                return Time.valueOf(hour + ":" + min + ":00");
            }
            // we have {8, am}
            int hour = Integer.parseInt(timeVal);
            if (!isAm && hour != 12) {
                hour += 12;
            }
            return Time.valueOf(hour + ":00:00");
        }
        if (parsedTime.length == 1) {
            // it means that the user put in
            // 8am or 12pm or 8:05am or 12:30pm
            String[] secondParse = parsedTime[0].split(":");
            if (secondParse.length == 1) {
                // if the user put in 8am or 12pm
                boolean isAm;
                Pattern p = Pattern.compile("\\d+");
                Matcher m = p.matcher(secondParse[0]);
                String hour = "";
                if (m.find()) {
                    hour = m.group();
                } else {
                    LOG.warn("Could not find number in {}", secondParse[0]);
                }
                Pattern amFinder = Pattern.compile(".*am\\b", Pattern.CASE_INSENSITIVE);
                Matcher amMatcher = amFinder.matcher(secondParse[0]);
                isAm = amMatcher.find();
                // we'll have to put it in the format of hh:mm:ss
                // we'll probably have something like 8 and whether it's am or pm
                if (!isAm) {
                    // if it's a pm value, 1pm, we have to add 12 + value
                    int tmpTimeVal = Integer.parseInt(hour);
                    if (tmpTimeVal != 12) {
                        tmpTimeVal+= 12;
                    }
                    // we now have 13
                    String newTimeVal = tmpTimeVal + ":00:00";
                    return Time.valueOf(newTimeVal);
                }
                return Time.valueOf(hour + ":00:00");
            }
            if (secondParse.length == 2) {
                // if the user put in 12:00pm or 1:30am
                // we would have something like {12, 00pm} or {1, 30am}
                // we have to handle the 00pm or the 30am
                boolean isAm;
                Pattern p = Pattern.compile("\\d+");
                Matcher m = p.matcher(secondParse[1]);
                String minute = "00";
                if (m.find()) {
                    minute = m.group();
                } else {
                    LOG.warn("Could not find a number in {}", secondParse[1]);
                }
                if (Integer.parseInt(minute) % 5 != 0) {
                    return null;
                }
                Pattern amFinder = Pattern.compile(".*am\\b", Pattern.CASE_INSENSITIVE);
                Matcher amMatcher = amFinder.matcher(secondParse[1]);
                isAm = amMatcher.find();
                if (!isAm) {
                    // it's a pm value, we'll have to adjust
                    int tmpHourVal = Integer.parseInt(secondParse[0]);
                    if (tmpHourVal != 12) {
                        tmpHourVal += 12;
                    }
                    String newTimeVal = tmpHourVal + ":" + minute + ":00";
                    return Time.valueOf(newTimeVal);
                }
                return Time.valueOf(secondParse[0] + ":" + minute + ":00");
            }
        }
        // default return 9 am
        return Time.valueOf("9:00:00");
    }
}
