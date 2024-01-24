package com.sha.perceptdrive.services;

import com.sha.perceptdrive.entities.HistoricalDriveTime;
import com.sha.perceptdrive.repository.HistoricalDriveTimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DriveTimeAnalysisService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final long TIME_BOUNDARIES = 10L;
    private final HistoricalDriveTimeRepository driveTimeRepo;

    public DriveTimeAnalysisService(final HistoricalDriveTimeRepository driveTimeRepo) {
        this.driveTimeRepo = driveTimeRepo;
    }

    // Method to pull the historical drive time data for a given origin and destination
    // returns {mean, mode, stdDeviation}
    public double[] getMeanModeDriveTime(final ZonedDateTime currentUTC, final String originAddress, final String destAddress) {
        // 2 - Query for the origin and destination in the table
        // 2a - Find time - 10 min
        LocalTime timeLeftBound = LocalTime.from(currentUTC.minusMinutes(TIME_BOUNDARIES));
        LocalTime timeRightBound = LocalTime.from(currentUTC.plusMinutes(TIME_BOUNDARIES));

        LOG.info("timeLeftBound (UTC): {}", timeLeftBound);
        LOG.info("timeRightBound (UTC): {}", timeRightBound);

        List<HistoricalDriveTime> allDriveTimes = driveTimeRepo.findHistoricalDriveTimesByOriginAndDestination(originAddress, destAddress);

        // Since we can't just query only for time, we'll have to do it programmatically here
        // We're only looking for times within the boundaries, date's don't matter
        // The Majority of queries are in UTC since all queries done on GCP are timestamped as UTC
        List<HistoricalDriveTime> driveTimes = new ArrayList<>();
        for (HistoricalDriveTime hdt : allDriveTimes) {
            LocalTime tmpTime = hdt.getDate().toLocalDateTime().toLocalTime();
            if (tmpTime.isAfter(timeLeftBound) && tmpTime.isBefore(timeRightBound)) {
                driveTimes.add(hdt);
            }
        }

        if (driveTimes.isEmpty()) {
            LOG.info("No historical drive times exist for the origin/destination: {} to {}", originAddress, destAddress);
            return new double[] {0, 0, 0};
        }
        if (driveTimes.size() <= 1) {
            LOG.info("There is only 1 datapoint to use, ignoring it");
            return new double[] {0, 0, 0};
        }
        LOG.info("Using {} data points to calculate mean/mode/stdDeviation", driveTimes.size());
        // 3 - Cycle through the driveTimes list to find the avg
        double tmpTime = 0.0;
        int count = 0;
        List<Integer> allTimes = new ArrayList<>();
        for (HistoricalDriveTime historicalDriveTime : driveTimes) {
            tmpTime += historicalDriveTime.getTravelTimeTraffic();
            count++;
            allTimes.add(historicalDriveTime.getTravelTimeTraffic());
        }
        double mode = getMode(allTimes);
        double avgTime =  tmpTime/count;
        double variance = getVariance(allTimes, avgTime);
        double stdDeviation = getStdDeviation(variance);
        LOG.info("Mean: {} | Mode: {} | stdDeviation: {}", avgTime, mode, stdDeviation);
        return new double[] {avgTime, mode, stdDeviation};
    }

    private int getMode(final List<Integer> times) {
        // Find the mode of the list of integers
        int maxValue = 0;
        int maxCount = 0;
        for (int t : times) {
            int c = 0;
            for (int k : times) {
                if (k == t) {
                    ++c;
                }
            }
            if (c > maxCount) {
                maxCount = c;
                maxValue = t;
            }
        }
        return maxValue;
    }

    private double getVariance(final List<Integer> times, final double mean) {
        // The variance is the mean of the squared differences.
        // for every data point, we subtract it from it's mean and square the difference - (x - u)^2
        int N = times.size();
        double sum = 0;
        for (int x : times) {
            sum += Math.pow((x - mean), 2);
        }
        return sum/N;
    }

    private double getStdDeviation(final double variance) {
        // Std Deviation is sqrt of variance
        return Math.sqrt(variance);
    }
}
