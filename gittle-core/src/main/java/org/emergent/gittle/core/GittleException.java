package org.emergent.gittle.core;

public class GittleException extends RuntimeException {

  public GittleException(String message) {
    super(message);
  }

  public GittleException(Throwable cause) {
    super(cause);
  }

  public GittleException(String message, Throwable cause) {
    super(message, cause);
  }
}
