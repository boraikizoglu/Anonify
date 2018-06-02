package com.example.os.anonify;

public class Message {
    private String message, createdAt, senderNickname, senderProfilePic;
    private User sender;

    Message(User sender, String message, String createdAt, String senderNickname, String senderProfilePic){
        this.sender = sender;
        this.message = message;
        this.createdAt = createdAt;
        this.senderNickname = senderNickname;
        this.senderProfilePic = senderProfilePic;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public User getSender() {
        return sender;
    }

    public String getSenderNickname(){
        return senderNickname;
    }

    public String getSenderProfilePic(){
        return senderProfilePic;
    }
}
