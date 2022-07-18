package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class WaitForConnect extends TimerTask {
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    public void run()
    {
      LOGGER.info("10 seconds have passed");
    }
  }