package com.sha.perceptdrive.entities;

import javax.persistence.*;
import java.util.Set;

@Entity
public class SavedLocation {
    @Id
    @Column(name="saved_location_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name="phone")
    private String phone;
    @Column(name="nickname")
    private String nickname;
    @Column(name="location")
    private String location;

    @OneToMany(mappedBy = "savedLocation")
    private Set<Schedule> schedule;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<Schedule> getSchedule() {
        return schedule;
    }

    public void setSchedule(Set<Schedule> schedule) {
        this.schedule = schedule;
    }
}
