import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.TimerTask;
import java.util.Timer;

public class WaitForConnect extends TimerTask {
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    public void run()
    {
      LOGGER.info("10 seconds have passed");
    }
  }