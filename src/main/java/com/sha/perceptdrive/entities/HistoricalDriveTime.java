package com.sha.perceptdrive.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity
public class HistoricalDriveTime {
    @Id
    @Column(name = "historical_drive_time_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String phone;
    private Timestamp date;
    private String origin;
    private String destination;
    private int travel_time_traffic; // we'll keep this as minutes

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserPhone() {
        return phone;
    }

    public void setUserPhone(final String user) {
        this.phone = user;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(final Timestamp date) {
        this.date = date;
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

    public int getTravelTimeTraffic() {
        return travel_time_traffic;
    }

    public void setTravelTimeTraffic(final int travelTimeTraffic) {
        this.travel_time_traffic = travelTimeTraffic;
    }

}
