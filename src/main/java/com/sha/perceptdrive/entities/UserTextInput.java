package com.sha.perceptdrive.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/*
 * Store users text input in an effort to train an ML model
 * to better detect where they are coming from, where they are
 * going, scheduling times, and saving locations.
 * All user text inputs will be captured
 */

@Entity
public class UserTextInput {
    @Id
    @Column(name = "user_text_input_id")
    private int id;
    private String user_text_input; // max of 200 characters 28-50 words
    private String text_type; // traffic_now, save, schedule

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getUserTextInput() {
        return user_text_input;
    }

    public void setUserTextInput(final String userTextInput) {
        this.user_text_input = userTextInput;
    }

    public String getTextType() {
        return text_type;
    }

    public void setTextType(final String text_type) {
        this.text_type = text_type;
    }
}
