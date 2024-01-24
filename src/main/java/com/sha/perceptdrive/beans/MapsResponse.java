package com.sha.perceptdrive.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MapsResponse {
    /*
     * This class will be used to store the response from Google Maps API.
     */
    private geocoded_waypoints[] geocoded_waypoints;
    private Routes[] routes;
    private String status;

    public MapsResponse() {}

    public static class geocoded_waypoints {
        @JsonProperty("geocoder_status")
        private String geocoder_status;
        @JsonProperty("partial_match")
        private boolean partial_match;
        @JsonProperty("place_id")
        private String place_id;
        @JsonProperty("types")
        private String[] types;

        public String getGeocoder_status() {
            return geocoder_status;
        }

        public void setGeocoder_status(String geocoder_status) {
            this.geocoder_status = geocoder_status;
        }

        public boolean getPartialMatch() {
            return partial_match;
        }

        public void setPartialMatch(final boolean partial_match) {
            this.partial_match = partial_match;
        }

        public String getPlace_id() {
            return place_id;
        }

        public void setPlace_id(String place_id) {
            this.place_id = place_id;
        }

        public String[] getTypes() {
            return types;
        }

        public void setTypes(String[] types) {
            this.types = types;
        }
    }

    public static class Routes {
        @JsonProperty("bounds")
        private Bounds bounds;
        @JsonProperty("copyrights")
        private String copyrights;
        @JsonProperty("legs")
        private Legs[] legs;
        @JsonProperty("overview_polyline")
        private OverviewPolyline overview_polyline;
        @JsonProperty("summary")
        private String summary;
        @JsonProperty("warnings")
        private String[] warnings;
        @JsonProperty("waypoint_order")
        private String[] waypoint_order;

        public Bounds getBounds() {
            return bounds;
        }

        public void setBounds(Bounds bounds) {
            this.bounds = bounds;
        }

        public String getCopyrights() {
            return copyrights;
        }

        public void setCopyrights(String copyrights) {
            this.copyrights = copyrights;
        }

        public Legs[] getLegs() {
            return legs;
        }

        public void setLegs(Legs[] legs) {
            this.legs = legs;
        }

        public OverviewPolyline getOverview_polyline() {
            return overview_polyline;
        }

        public void setOverview_polyline(OverviewPolyline overview_polyline) {
            this.overview_polyline = overview_polyline;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String[] getWarnings() {
            return warnings;
        }

        public void setWarnings(String[] warnings) {
            this.warnings = warnings;
        }

        public String[] getWaypoint_order() {
            return waypoint_order;
        }

        public void setWaypoint_order(String[] waypoint_order) {
            this.waypoint_order = waypoint_order;
        }

    }

    public static class Bounds {
        @JsonProperty("northeast")
        private Northeast northeast;
        @JsonProperty("southwest")
        private Southwest southwest;

        public Northeast getNortheast() {
            return northeast;
        }

        public void setNortheast(Northeast northeast) {
            this.northeast = northeast;
        }

        public Southwest getSouthwest() {
            return southwest;
        }

        public void setSouthwest(Southwest southwest) {
            this.southwest = southwest;
        }

    }

    public static class Northeast {
        @JsonProperty("lat")
        private double lat;
        @JsonProperty("lng")
        private double lng;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

    }

    public static class Southwest {
        @JsonProperty("lat")
        private double lat;
        @JsonProperty("lng")
        private double lng;
        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }

    public static class Legs {
        @JsonProperty("distance")
        private Distance distance;
        @JsonProperty("duration")
        private Duration duration;
        @JsonProperty("end_address")
        private String end_address;
        @JsonProperty("duration_in_traffic")
        private Duration duration_in_traffic;
        @JsonProperty("end_location")
        private EndLocation end_location;
        @JsonProperty("start_address")
        private String start_address;
        @JsonProperty("start_location")
        private StartLocation start_location;
        @JsonProperty("steps")
        private Steps[] steps;
        @JsonProperty("traffic_speed_entry")
        private String[] traffic_speed_entry;
        @JsonProperty("via_waypoint")
        private String[] via_waypoint;
        public Distance getDistance() {
            return distance;
        }

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public Duration getDurationInTraffic() {
            return duration_in_traffic;
        }

        public void setDurationInTraffic(final Duration durationInTraffic) {
            this.duration_in_traffic = durationInTraffic;
        }

        public String getEnd_address() {
            return end_address;
        }

        public void setEnd_address(String end_address) {
            this.end_address = end_address;
        }

        public EndLocation getEnd_location() {
            return end_location;
        }

        public void setEnd_location(EndLocation end_location) {
            this.end_location = end_location;
        }

        public String getStart_address() {
            return start_address;
        }

        public void setStart_address(String start_address) {
            this.start_address = start_address;
        }

        public StartLocation getStart_location() {
            return start_location;
        }

        public void setStart_location(StartLocation start_location) {
            this.start_location = start_location;
        }

        public Steps[] getSteps() {
            return steps;
        }

        public void setSteps(Steps[] steps) {
            this.steps = steps;
        }

        public String[] getTraffic_speed_entry() {
            return traffic_speed_entry;
        }

        public void setTraffic_speed_entry(String[] traffic_speed_entry) {
            this.traffic_speed_entry = traffic_speed_entry;
        }

        public String[] getVia_waypoint() {
            return via_waypoint;
        }

        public void setVia_waypoint(String[] via_waypoint) {
            this.via_waypoint = via_waypoint;
        }

    }

    public static class Distance {
        @JsonProperty("text")
        private String text;
        @JsonProperty("value")
        private int value;
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

    }

    public static class Duration {
        @JsonProperty("text")
        private String text;
        @JsonProperty("value")
        private int value;
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

    }

    public static class EndLocation {
        @JsonProperty("lat")
        private double lat;
        @JsonProperty("lng")
        private double lng;
        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

    }

    public static class StartLocation {
        @JsonProperty("lat")
        private double lat;
        @JsonProperty("lng")
        private double lng;
    }

    public static class Steps {
        @JsonProperty("distance")
        private Distance distance;
        @JsonProperty("duration")
        private Duration duration;
        @JsonProperty("end_location")
        private EndLocation end_location;
        @JsonProperty("html_instructions")
        private String html_instructions;
        @JsonProperty("polyline")
        private Polyline polyline;
        @JsonProperty("start_location")
        private StartLocation start_location;
        @JsonProperty("travel_mode")
        private String travel_mode;
        @JsonProperty("maneuver")
        private String maneuver;
        public Distance getDistance() {
            return distance;
        }

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public EndLocation getEnd_location() {
            return end_location;
        }

        public void setEnd_location(EndLocation end_location) {
            this.end_location = end_location;
        }

        public String getHtml_instructions() {
            return html_instructions;
        }

        public void setHtml_instructions(String html_instructions) {
            this.html_instructions = html_instructions;
        }

        public Polyline getPolyline() {
            return polyline;
        }

        public void setPolyline(Polyline polyline) {
            this.polyline = polyline;
        }

        public StartLocation getStart_location() {
            return start_location;
        }

        public void setStart_location(StartLocation start_location) {
            this.start_location = start_location;
        }

        public String getTravel_mode() {
            return travel_mode;
        }

        public void setTravel_mode(String travel_mode) {
            this.travel_mode = travel_mode;
        }

        public String getManeuver() {
            return maneuver;
        }

        public void setManeuver(String maneuver) {
            this.maneuver = maneuver;
        }

    }

    public static class Polyline {
        @JsonProperty("points")
        private String points;
        public String getPoints() {
            return points;
        }

        public void setPoints(String points) {
            this.points = points;
        }

    }

    public static class OverviewPolyline {
        @JsonProperty("points")
        private String points;
        public String getPoints() {
            return points;
        }

        public void setPoints(String points) {
            this.points = points;
        }

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public geocoded_waypoints[] getGeocoded_waypoints() {
        return geocoded_waypoints;
    }

    public void setGeocoded_waypoints(geocoded_waypoints[] geocoded_waypoints) {
        this.geocoded_waypoints = geocoded_waypoints;
    }

    public Routes[] getRoutes() {
        return routes;
    }

    public void setRoutes(Routes[] routes) {
        this.routes = routes;
    }

}
