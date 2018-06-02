package com.example.os.anonify;

import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class User extends ListViewElement{

    private String uid, email, username, distance, latitude, longitude, profilePicUrl;

    User(String uid, String email, String username, String distance, String latitude, String longitude, String profilePicUrl){
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.distance = distance;
        this.latitude = latitude;
        this.longitude = longitude;
        this.profilePicUrl = profilePicUrl;
    }

    public String getUid(){
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getTitle() {
        return username;
    }

    public String getDistance() {
        return distance;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public String getChatKey(){
        return email;
    }

}