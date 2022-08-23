package com.uwaterloo.iqc.qnl;

import java.util.HashMap;
import java.util.Timer;

public class TimerWrapper{
    private HashMap<String, Timer> startNodeTimers;

    public TimerWrapper(){
        startNodeTimers = new HashMap<String,Timer>();
    }

    public HashMap<String, Timer> getStartNodeTimers(){
        return startNodeTimers;
    }

    public Timer getTimer(String deviceID){
        return startNodeTimers.get(deviceID);
    }

    public int getSize(){
        return startNodeTimers.size();
    }

    public void addTimer(String deviceID){
        startNodeTimers.put(deviceID, new Timer());
    }

    public void setTimers(HashMap<String, Timer> timers){
        startNodeTimers = timers;
    }

    public void removeTimer(String deviceID){
        startNodeTimers.get(deviceID).cancel();
        startNodeTimers.remove(deviceID);
    }
}