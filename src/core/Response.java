package core;

import java.util.stream.Collectors;

import static core.HttpRequestRegEx.CRLF;
import static util.Helper.getServerTime;
import static util.StringUtils.addPostfix;
import static util.StringUtils.defaultString;

public class Response extends HttpMessage {

  public Response() {
    httpVersion = "HTTP/1.1";
  }

  public Response(Request request) {
    this();
    if (request.httpVersion != null)
      httpVersion = request.httpVersion;

    requestMethod = request.requestMethod;

    if (request.responseStatusCode != null) {
      generateStandardResponse(request.responseStatusCode);
      return;
    }
  }

  @Override
  public void setBody(String body) {
    super.setBody(body);
    setHeader("Content-Length", "" + calculateContentLength());

    if ("HEAD".equals(requestMethod))
      super.setBody(null);
  }

  public String generateMessage() {
    validateResponse();

    String headersString = headers.entrySet().stream()
      .map((entry) -> entry.getKey() + ": " + entry.getValue())
      .collect(Collectors.joining(CRLF));

    return httpVersion + " " + responseStatusCode + CRLF +
      addPostfix(headersString, CRLF) +
      CRLF +
      defaultString(body);
  }

  public void generateStandardResponse(HttpStatusCode code) {
    responseStatusCode = code;
    setBody(responseStatusCode.toString());
    setHeader("Content-Type", "text/html; charset=" + bodyCharset);
    setHeader("Last-modified", getServerTime());
  }

  public void validateResponse() {
    if (responseStatusCode == null)
      throw new RuntimeException("Cannot generate a valid HTTP response without status code");

    if (httpVersion == null)
      throw new RuntimeException("Cannot generate a valid HTTP response without HTTP version");

    if (!contentHeadersAreCorrect())
      throw new RuntimeException("Content-* headers are not allowed without body in response to non-HEAD requests");
  }
}