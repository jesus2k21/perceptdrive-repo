package com.sha.perceptdrive.repository;

import com.sha.perceptdrive.entities.HistoricalDriveTime;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;

public interface HistoricalDriveTimeRepository extends CrudRepository<HistoricalDriveTime, Integer> {

    List<HistoricalDriveTime> findHistoricalDriveTimeByPhone(final String user);
    List<HistoricalDriveTime> findHistoricalDriveTimesByOriginAndDestination(final String origin, final String dest);
    List<HistoricalDriveTime> findHistoricalDriveTimesByDateAndOriginAndDestination(final Timestamp time, final String origin, final String dest);
    List<HistoricalDriveTime> findHistoricalDriveTimesByDateBetweenAndOriginAndDestination(final Timestamp timeA, final Timestamp timeB, final String origin, final String dest);
    List<HistoricalDriveTime> findHistoricalDriveTimesByOriginAndDestinationAndDateBetween(final String origin, final String dest, final Timestamp timeA, final Timestamp timeB);
}
