package org.example.authserver.util.shutdown;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class ShutDownUtil {
  public static void registerShutDownHookForTermAndInt(SignalHandler handler) {
    Signal.handle(new Signal("INT"), handler);
    Signal.handle(new Signal("TERM"), handler);
  }
}
