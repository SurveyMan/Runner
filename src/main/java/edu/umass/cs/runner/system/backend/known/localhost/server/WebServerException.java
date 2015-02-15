package edu.umass.cs.runner.system.backend.known.localhost.server;

/**
 * @author jfoley.
 */
public class WebServerException extends Exception {
  public WebServerException(Exception reason) {
    super(reason);
  }
}
