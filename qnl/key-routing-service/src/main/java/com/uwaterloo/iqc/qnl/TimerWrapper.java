package com.uwaterloo.iqc.qnl;

import java.util.HashMap;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerWrapper{
    private HashMap<String, Timer> startNodeTimers;
    private static Logger LOGGER = LoggerFactory.getLogger(TimerWrapper.class);

    public TimerWrapper(){ // creates a wrapper for our HashMap of timers, so we can edit and modify the list from any class.
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
        LOGGER.info("creation of a new timer for " + deviceID + "!");
        startNodeTimers.put(deviceID, new Timer());
    }

    public void setTimers(HashMap<String, Timer> timers){
        startNodeTimers = timers;
    }

    public void removeTimer(String deviceID){
        Timer t = startNodeTimers.remove(deviceID);
        if(t != null){
            t.cancel();
            LOGGER.info("timer for " + deviceID + " has been successfully killed.");
        }
    }
}