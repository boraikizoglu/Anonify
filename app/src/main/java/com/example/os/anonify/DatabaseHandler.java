package com.example.os.anonify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DatabaseVersion=1;
    private static final String DatabaseName="db_myapp";
    private static final String CreateUserTable_Task="Create Table user(userKey Text, distance Text, email Text,latitude Text, longitude Text, profilePicUrl Text, username Text)";
    private static final String CreateAnonTable_Task="Create Table anon(anonKey Text, latitude Text, longitude Text, title Text)";

    private static  DatabaseHandler mInstance = null;
    public static DatabaseHandler getInstance(Context context){
        if(mInstance==null){
            mInstance = new DatabaseHandler(context.getApplicationContext());
        }
        return mInstance;
    }

    public DatabaseHandler(Context context){
        super(context, DatabaseName, null, DatabaseVersion);
    }
    @Override
    public  void onCreate(SQLiteDatabase db){
        db.execSQL(CreateUserTable_Task);
        db.execSQL(CreateAnonTable_Task);
    }
    @Override
    public  void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onCreate(db);
    }

    public boolean userExits(String uid){
        SQLiteDatabase db   = this.getWritableDatabase();
        String query = "SELECT userKey from user where userKey = '" + uid + "'";
        Cursor cursor = db.rawQuery(query, null);
        boolean b = cursor.getCount() == 0 ? false : true;
        cursor.close();
        return b;
    }

    public void insertUser(User u){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userKey",u.getUid());
        values.put("distance",u.getDistance());
        values.put("email",u.getEmail());
        values.put("latitude",u.getLatitude());
        values.put("longitude",u.getLongitude());
        values.put("profilePicUrl",u.getProfilePicUrl());
        values.put("username",u.getUsername());
        db.insert("user",null,values);
    }

    public ArrayList<ListViewElement> getUserArrayList(){
        ArrayList userArrayList = new ArrayList<>();
        SQLiteDatabase db   = this.getWritableDatabase();
        String query = "Select * from user";
        try (Cursor cursor = db.rawQuery(query, null);) {
            while (cursor.moveToNext()) {
                String userKey = cursor.getString(0);
                String distance = cursor.getString(1);
                String email = cursor.getString(2);
                String latitude = cursor.getString(3);
                String longitude = cursor.getString(4);
                String profilePicUrl = cursor.getString(5);
                String username = cursor.getString(6);
                User u = new User(userKey, email, username, distance, latitude, longitude, profilePicUrl);
                userArrayList.add(u);
            }
            cursor.close();
        }
        return userArrayList;
    }

    public boolean anonChatExits(String anonChatRoomID){
        SQLiteDatabase db   = this.getWritableDatabase();
        String query = "SELECT anonKey from anon where anonKey = '" + anonChatRoomID + "'";
        Cursor cursor = db.rawQuery(query, null);
        boolean b = cursor.getCount() == 0 ? false : true;
        cursor.close();
        return b;
    }

    public void insertAnonChat(Anon a){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("anonKey", a.getChatKey());
        values.put("latitude", a.getLatitude());
        values.put("longitude", a.getLongitude());
        values.put("title", a.getTitle());
        db.insert("anon",null,values);
    }

    public ArrayList<ListViewElement> getAnonArrayList(){
        ArrayList anonArrayList = new ArrayList<>();
        SQLiteDatabase db   = this.getWritableDatabase();
        String query = "Select * from anon";
        try (Cursor cursor = db.rawQuery(query, null);) {
            while (cursor.moveToNext()) {
                String anonKey = cursor.getString(0);
                String latitude = cursor.getString(1);
                String longitude = cursor.getString(2);
                String title = cursor.getString(3);
                Anon a = new Anon(latitude, longitude, title, anonKey);
                anonArrayList.add(a);
            }
            cursor.close();
        }
        return anonArrayList;
    }

}