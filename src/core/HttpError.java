package core;

public class HttpError extends RuntimeException {
  final HttpStatusCode errorCode;

  HttpError(HttpStatusCode errorCode) {
    this.errorCode = errorCode;
  }

  HttpStatusCode getErrorCode() {
    return errorCode;
  }
}