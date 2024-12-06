package org.example.authserver.util.shutdown;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class ShutDownHandler implements SignalHandler {
  @Override
  public void handle(Signal sig) {
    log.debug("START ---- ShutDownHandler.handle() {}  ---- START", sig.getNumber());
    if (StringUtils.isNotBlank(System.getenv("SHUTDOWN_SCRIPT"))) {
      executeShutdownScript(System.getenv("SHUTDOWN_SCRIPT"));
    } else {
      log.debug("No shutdown script found in env variable SHUTDOWN_SCRIPT");
    }
    log.debug("END ---- ShutDownHandler.handle() ---- END");
    System.exit(128 + sig.getNumber());
  }

  private void executeShutdownScript(final String scriptWithFullPath) {
    try {
      log.debug("{}: Running shutdown script", scriptWithFullPath);

      ProcessBuilder processBuilder = new ProcessBuilder(scriptWithFullPath);
      Process process = processBuilder.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      while ((line = reader.readLine()) != null) {
        log.trace(scriptWithFullPath + " : " + line);
      }

      int exitCode = process.waitFor();
      log.debug("{} Exited with code: {} ", scriptWithFullPath, exitCode);
    } catch (Exception e) {
      log.error("Error executing shutdown script: " + scriptWithFullPath, e);
    }
  }
}
