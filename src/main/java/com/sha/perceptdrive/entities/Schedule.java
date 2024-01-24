package com.sha.perceptdrive.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Time;
import java.sql.Date;

@Entity
@Table(name="schedule")
public class Schedule {
    @Id
    @Column(name="schedule_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name="phone")
    private String phone;
    @Column(name="origin")
    private String origin;
    @Column(name="destination")
    private String destination;
    @Column(name="days")
    private String days;
    @Column(name="time")
    private Time time;
    @Column(name="days_paused")
    private int daysPaused;
    @Column(name="end_pause_date")
    private Date endPauseDate;

    @ManyToOne
    @JoinColumn(name = "saved_location_id")
    private SavedLocation savedLocation;

    public Schedule() {}

    public Schedule(final SavedLocation savedLocation) {
        this.savedLocation = savedLocation;
    }


    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(final String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(final String destination) {
        this.destination = destination;
    }

    public String getDays() {
        return days;
    }

    public void setDays(final String excludedDays) {
        this.days = excludedDays;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(final Time time) {
        this.time = time;
    }

    public SavedLocation getSavedLocation() {
        return savedLocation;
    }

    public void setSavedLocation(final SavedLocation savedLocation) {
        this.savedLocation = savedLocation;
    }

    public int getDaysPaused() {
        return daysPaused;
    }

    public void setDaysPaused(final int daysPaused) {
        this.daysPaused = daysPaused;
    }

    public Date getEndPauseDate() {
        return endPauseDate;
    }

    public void setEndPauseDate(final Date endTimePaused) {
        this.endPauseDate = endTimePaused;
    }
}
