package io.evolution.downtohang;

import android.location.Location;

/**
 *
 * This is the User object, contains information on an individual user.
 * It will contain the following:
 *
 * UUID number
 * Username
 * Status (Availability)
 * Hangout Status
 * Latitude
 * Longitude
 */
public class User {

    // unique id of user
    private String uuid ;
    private String username;
    // String is either 0 or the UUID of the leader of the hangout they are in
    private String hangoutStatus;
    // available, not available, in a hangout, busy etc.
    private int status;
    private double latitude;
    private double longitude;
    private boolean isSelected;

    /**
     * Creates a user object
     * @param uuid the user's uuid
     * @param username the user's name
     * @param status the user's status
     * @param hangoutStatus the user's hangout status
     * @param latitude the user's latitude coordinate
     * @param longitude the user's longitude coordinate
     */
    public User(String uuid, String username, int status, String hangoutStatus, double latitude,
                double longitude) {
        this.uuid = uuid;
        this.username = username;
        this.status = status;
        this.hangoutStatus = hangoutStatus;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return this user's uuid
     */
    public String getUUID(){
        return uuid;
    }

    /**
     * @return this user's name
     */
    public String getUsername(){
        return username;
    }

    /**
     * @return this user's hangout status
     */
    public String getHangoutStatus(){
        return hangoutStatus;
    }

    /**
     * @return this user's status
     */
    public Integer getStatus(){
        return status;
    }

    /**
     * @return the user's latitude coordinate
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return the user's longitude coordinate
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return true if the user is selected in the create hangout screen.
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * @return an android location object based on this user's latitude and longitude coordinates
     */
    public Location getLocation() {
        Location l = new Location("");
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        return l;
    }

    /**
     * Check if one user is equal to another user. Two users are considered
     * equal if they have the same UUID.
     * @param o an object
     * @return true if the given object is equal to this user.
     */
    public boolean equals(Object o) {
        if(o instanceof User) {
            User u = (User) o;
            if(u.getUUID().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the hash code of this user (the uuid's hash code)
     */
    public int hashCode() {
        return uuid.hashCode();
    }


    /**
     * Set the user's uuid
     * @param id the new uuid
     */
    public void setId(String id){
        this.uuid = id;
    }

    /**
     * Set the user's username
     * @param username the new username
     */
    public void setUsername(String username){
        this.username = username;
    }

    /**
     * Set the user's hangout status
     * @param hangoutStatus the new hangout status
     */
    public void setHangoutStatus(String hangoutStatus){
        this.hangoutStatus = hangoutStatus;
    }

    /**
     * Set the user's status
     * @param status the new status
     */
    public void setStatus(int status){
        this.status = status;
    }

    /**
     * Set the user's latitude coordinate
     * @param latitude the new latitude coordinate
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Set the user's longitude coordinate
     * @param longitude the new longitude coordinate
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Set whether or not this user is selected
     * @param isSelected the new selected status
     */
    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
