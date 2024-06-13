package com.example.sca.History.RecyclerViewMatierials;

public class HistoryObject {
    private String deliveryID;
    private String timeID;

    public HistoryObject(String rideID, String time){
        this.deliveryID = rideID;
        this.timeID = time;
    }

    public String getRideID(){
        return deliveryID;
    }

    public String getTime(){
        return timeID;
    }
}
