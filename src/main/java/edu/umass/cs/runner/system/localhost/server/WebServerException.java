package edu.umass.cs.runner.system.localhost.server;

/**
 * @author jfoley.
 */
public class WebServerException extends Exception {
  public WebServerException(Exception reason) {
    super(reason);
  }
}
