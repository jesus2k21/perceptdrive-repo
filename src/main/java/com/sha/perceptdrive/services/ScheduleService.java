package com.sha.perceptdrive.services;

import com.sha.perceptdrive.beans.MapsResponse;
import com.sha.perceptdrive.beans.RouteInformation;
import com.sha.perceptdrive.controllers.TwilioSendController;
import com.sha.perceptdrive.entities.SavedLocation;
import com.sha.perceptdrive.entities.Schedule;
import com.sha.perceptdrive.repository.SavedLocationRepository;
import com.sha.perceptdrive.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.util.List;

@Service
public class ScheduleService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String WEEKDAYS = "weekdays";
    private static final String WEEKENDS = "weekends";
    private final DirectionsService directionsService;
    private final ScheduleRepository scheduleRepo;
    private final SavedLocationRepository savedLocationRepo;
    private final TwilioSendController sendSms;
    private final DriveTimeAnalysisService timeAnalysisService;

    public ScheduleService(final DirectionsService directionsService, final ScheduleRepository scheduleRepo,
                           final TwilioSendController sendSms, final SavedLocationRepository savedLocationRepo,
                           final DriveTimeAnalysisService timeAnalysisService) {
        this.directionsService = directionsService;
        this.scheduleRepo = scheduleRepo;
        this.sendSms = sendSms;
        this.savedLocationRepo = savedLocationRepo;
        this.timeAnalysisService = timeAnalysisService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void querySchedules() {
        Instant now = Instant.now();
        ZonedDateTime currentUTC = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
        LocalDateTime time = LocalDateTime.ofInstant(now, ZoneId.of("America/Chicago"));
        Date date = Date.valueOf(time.toLocalDate());
        DayOfWeek day = time.getDayOfWeek();
        boolean isWeekend = isWeekend(day);
        String createSqlTime = time.getHour() + ":" + time.getMinute() + ":00";
        Time sqlTime = Time.valueOf(createSqlTime);
        LOG.info("Querying the database. Time is {}", sqlTime);

        // find all users that said they wanted texts sent at that hour
        List<Schedule> schedulesForThisTime = scheduleRepo.findAllByTime(sqlTime);
        if (schedulesForThisTime.isEmpty()) {
            LOG.info("No schedules found for {}", sqlTime);
            return;
        }
        for (Schedule schedule : schedulesForThisTime) {
            // we'll check if their schedule is paused
            boolean isPaused = isPaused(date, schedule.getEndPauseDate());
            // we'll go through all Schedules and check for the supported keywords
            // weekdays, everyday, weekends
            if (isPaused) {
                // The schedule has notifications paused
                break;
            }
            if (isWeekend && schedule.getDays().equals(WEEKDAYS)) {
                // this schedule is for a weekday and it is a weekend
                break;
            }
            if (!isWeekend && schedule.getDays().equals(WEEKENDS)) {
                // this schedule is for a weekend and it is a weekday
                break;
            }
            RouteInformation routeInformation = getRouteInformation(schedule);
            if (routeInformation == null) {
                LOG.warn("Will not send {} their scheduled text as their saved origin/destination addresses are not valid.", schedule.getPhone());
                String msgBody = String.format("Looks like your origin or destination addresses are coming up moot. Please try changing them by giving the command \"Change school address to 2423 Blinn Blvd, Bryan, TX.\".\nOrigin: %s\nDestination: %s", schedule.getOrigin(), schedule.getDestination());
                sendSms.sendSms("+" + schedule.getPhone(), msgBody);
                return;
            }
            LOG.info("Sending SMS to {}", schedule.getPhone());
            LOG.info("Google maps Url for user {} - to {} from {}: {}", schedule.getPhone(), schedule.getDestination(), schedule.getOrigin(), routeInformation.getGoogleMapsUrl());
            String[] addresses = getOriginDestAddress(schedule.getPhone(), schedule.getOrigin(), schedule.getDestination());
            if (addresses == null) {
                LOG.info("something went wrong...");
                return;
            }
            // We'll only calculate the mean/mode using times that are within 10 minutes of the time of the query
            LOG.info("Current UTC: {}", currentUTC);
            double[] meanModeStdDevTimes = timeAnalysisService.getMeanModeDriveTime(currentUTC, addresses[0], addresses[1]);

            sendSms.sendRouteInfoSms(schedule.getPhone(), routeInformation, meanModeStdDevTimes, routeInformation.getGoogleMapsUrl(), schedule.getOrigin(), schedule.getDestination());
        }
    }

    // Run updates to days_paused every day at midnight
    @Scheduled(cron = "@midnight") // cron = "0 0 0 * * *"
    public void updateAllSchedulePauseDays() {
        List<Schedule> schedulesToUpdate = scheduleRepo.findAllByDaysPausedIsGreaterThan(0);
        if (schedulesToUpdate.isEmpty()) {
            LOG.info("No schedules need updating");
            return;
        }
        for (Schedule schedule : schedulesToUpdate) {
            int decrementedDays = schedule.getDaysPaused() - 1;
            schedule.setDaysPaused(decrementedDays);
            scheduleRepo.save(schedule);
        }
        LOG.info("Updated {} schedules", schedulesToUpdate.size());
    }

    private boolean isPaused(final Date currentDate, final Date endDate) {
        if (currentDate == null || endDate == null) {
            return false;
        }
        int ret = currentDate.compareTo(endDate);
        // if 1, then that means the currentDate is after endDate
        // if -1, then that means that currentDate is before endDate
        return ret == -1;
    }

    private RouteInformation getRouteInformation(final Schedule schedule) {
        // get Origin is the origin nickname and destination is the destination nickname
        // we'll have to retrieve the addresses from the DB
        String[] originDestAddress = getOriginDestAddress(schedule.getPhone(), schedule.getOrigin(), schedule.getDestination());
        if (originDestAddress == null) {
            return null;
        }

        MapsResponse mapsResponse = directionsService.getMapsDirections(schedule.getPhone(), originDestAddress[0],
                originDestAddress[1]);

        if (mapsResponse == null) {
            LOG.warn("Could not find an address for origin: {} and destination: {}", schedule.getOrigin(), schedule.getDestination());
            return null;
        }
        String trafficTime = mapsResponse.getRoutes()[0].getLegs()[0].getDurationInTraffic().getText();
        RouteInformation routeInformation = new RouteInformation();
        routeInformation.setTravelTime(mapsResponse.getRoutes()[0].getLegs()[0].getDuration().getText());
        routeInformation.setDistance(mapsResponse.getRoutes()[0].getLegs()[0].getDistance().getText());
        routeInformation.setTraffic(trafficTime);
        routeInformation.setGoogleMapsUrl(directionsService.getGoogleMapUrl(originDestAddress[0], originDestAddress[1]));
        return routeInformation;
    }

    private boolean isWeekend(final DayOfWeek day) {
        return day.name().equals("SATURDAY") || day.name().equals("SUNDAY");
    }

    private String[] getOriginDestAddress(final String phone, final String originNickName, final String destNickName) {
        SavedLocation origin = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(phone, originNickName);
        SavedLocation dest = savedLocationRepo.findSavedLocationByPhoneAndNicknameIgnoreCase(phone, destNickName);
        if (origin == null || dest == null) {
            LOG.error("Could not find users saved locations. Perhaps they were deleted?");
            return null;
        }
        // we should only get one, but it's currently possible to add duplicates
        // for now, we'll only return the first one found
        return new String[] {origin.getLocation(), dest.getLocation()};
    }
}
