package com.sha.perceptdrive.repository;

import com.sha.perceptdrive.entities.Schedule;
import org.springframework.data.repository.CrudRepository;

import java.sql.Time;
import java.util.List;

public interface ScheduleRepository extends CrudRepository<Schedule, Integer> {
    List<Schedule> findAllByTime(final Time time);
    List<Schedule> findAllByPhoneAndTime(final String phone, final Time time);
    Schedule findScheduleByPhoneAndTime(final String phone, final Time time);
    List<Schedule> findAllByPhone(final String phone);
    List<Schedule> findAllByDaysPausedIsGreaterThan(final int minDays);
}
