package core;

public enum HttpStatusCode {

    OK (200, "OK"),
    BAD_REQUEST (400, "Bad Request"),
    FORBIDDEN (403, "Forbidden"),
    NOT_FOUND (404, "Not Found"),
    REQUEST_TIMEOUT (408, "Request Timeout"),
    REQUEST_URI_TOO_LONG (414, "Request-URI Too Long"),
    INTERNAL_SERVER_ERROR (500, "Internal Server Error"),
    NOT_IMPLEMENTED (501, "Not Implemented"),
    HTTP_VERSION_NOT_SUPPORTED (505, "HTTP Version Not Supported");

    private final int code;
    private final String reasonPhrase;

    private HttpStatusCode(int code, String reasonPhrase) {
      this.code = code;
      this.reasonPhrase = reasonPhrase;
    }

    public int getCode() {
      return code;
    }

    public String getReasonPhrase() {
      return reasonPhrase;
    }

    @Override
    public String toString() {
      return code + " " + reasonPhrase;
    }

}