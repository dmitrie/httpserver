package core;

public enum HttpStatusCode {

    OK (200, "OK"),
    BAD_REQUEST (400, "Bad core.Request"),
    NOT_FOUND (404, "Not Found"),
    INTERNAL_SERVER_ERROR (500, "Internal core.Server Error"),
    NOT_IMPLEMENTED (501, "Not Implemented"),
    REQUEST_TIMEOUT (408, "Request Timeout");

    private final int code;
    private final String name;

    private HttpStatusCode(int code, String name) {
      this.code = code;
      this.name = name;
    }

    public int getCode() {
      return code;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return code + " " + name;
    }

}