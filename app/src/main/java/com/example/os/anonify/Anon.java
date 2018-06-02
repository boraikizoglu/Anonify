package com.example.os.anonify;

import java.util.ArrayList;

public class Anon extends ListViewElement {

    private String latitude, longitude, title, chatID, profilePicUrl="";
    private ArrayList<Message> messages;

    public Anon(String latitude, String longitude, String title, String chatID) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.chatID = chatID;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public String getChatKey(){
        return chatID;
    }
}
