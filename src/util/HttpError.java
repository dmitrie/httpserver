package util;

public class HttpError extends RuntimeException {
  public final HttpStatusCode errorCode;

  public HttpError(HttpStatusCode errorCode) {
    this.errorCode = errorCode;
  }

  public HttpStatusCode getErrorCode() {
    return errorCode;
  }
}