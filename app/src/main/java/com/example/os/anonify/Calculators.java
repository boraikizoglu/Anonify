package com.example.os.anonify;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Calculators {

    ///We always insert with GMT time to database.
    //This function converts GMT to device's time zone
    //Then returns hour and minute
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String returnHourAndMinute(String time){
        ZonedDateTime gmtTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd-H-mm-ss-SSS")).atZone(ZoneId.of("GMT"));
        LocalDateTime localTime = gmtTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        String hour = Integer.toString(localTime.getHour());
        String minute = Integer.toString(localTime.getMinute());

        if(Integer.parseInt(hour) < 10) hour = "0" + hour;
        if(Integer.parseInt(minute) < 10) minute = "0" + minute;

        return hour + ":" + minute;
    }

    //returns time with GMT time zone
    public static String getTime(){
        Date now = new Date();
        new SimpleDateFormat("yyyy-MM-dd-H-mm-ss-SSS").format(Calendar.getInstance().getTime());
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-H-mm-ss-SSS");
        df.setTimeZone(TimeZone.getTimeZone("Etc/GMT"));
        return df.format(now);
    }

    // ---------- Calculates distance between two coordinate ----------

    public static Double returnDistance(Double lat1, Double lon1, Double lat2, Double lon2, char unit) {
        Double theta = lon1 - lon2;
        Double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        //K means km. M means Miles. N means Nautical Miles
        if (unit == 'K') {
            return dist * 1.609344;
        }
        else if (unit == 'N') {
            return dist * 0.8684;
        }
        else if(unit == 'M'){
            return dist;
        }
        return -1.0;
    }

    //This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    //This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    // ---------------------------
}
