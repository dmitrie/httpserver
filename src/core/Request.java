package core;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static core.HttpRequestRegEx.*;
import static core.HttpStatusCode.*;

public class Request {
  private String method;
  private String path;
  private String protocol = "HTTP/1.1";
  private Map<String, String> headers = new LinkedCaseInsensitiveMap();
  private HttpStatusCode errorCode;
  private String body;

  public Request() {}

  public Request(InputStream in) throws IOException {
    try {
      String[] requestLineAndHeaders = readRequestLineAndHeaders(in).split(CRLF,2);
      setRequestLineMembers(requestLineAndHeaders[0]);

      if (requestLineAndHeaders.length == 2) {
        for (String header : replaceMultipleLWSWithSingleSpace(requestLineAndHeaders[1]).split(CRLF))
          setHeader(header);
      }

      if (getHeader("TRANSFER-ENCODING") != null)
        throw new HttpError(NOT_IMPLEMENTED);

      if (getHeader("HOST") == null && path.charAt(0) == '/')
        throw new HttpError(BAD_REQUEST);

      setBody(readBody(in));
    } catch (HttpError e) {
     setErrorCode(e.getErrorCode());
    }
  }

  public String readRequestLineAndHeaders(InputStream in) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();

    int byteRead;
    while ((byteRead = in.read()) != -1) {
      if (stringBuilder.toString().equals(CRLF))
        stringBuilder = new StringBuilder();

      stringBuilder.append((char) byteRead);

      if ((CRLF+CRLF).equals(stringBuilder.substring(Math.max(0,stringBuilder.length()-4))))
        break;
    }

    return stringBuilder.toString();
  }

  public String readBody(InputStream in) throws IOException {
    String contentLength = getHeader("CONTENT-LENGTH");
    //TODO should return 400 error if there is body and no Content-Length header, think how to differentiate it from time-out
    if (contentLength == null)
      return null;

    int numericContentLength = Integer.parseInt(contentLength);
    if (numericContentLength == 0)
      return "";

    byte[] buffer = new byte[numericContentLength];
    int bytesActuallyRead = in.read(buffer, 0, numericContentLength);
    return new String(buffer, 0, bytesActuallyRead);
  }

  public void setRequestLineMembers(String requestLine) {
    if (!validateRequestLineFormat(requestLine))
      throw new HttpError(BAD_REQUEST);

    String[] splitRequestLine = requestLine.split(" ");

    setMethod(splitRequestLine[0]);
    setPath(splitRequestLine[1]);
    setProtocol(splitRequestLine[2]);
  }

  public String getHeader(String header) {
    return getHeaders().get(header);
  }

  public void setHeader(String header, String value) {
    String headerValue = getHeader(header);

    getHeaders().put(header, (headerValue == null ? "" : headerValue + ", ") + value.trim());
  }

  public void setHeader(String headerValuePair) {
    if (!validateHeader(headerValuePair))
      throw new HttpError(BAD_REQUEST);

    String[] headerValuePairSplitByColon = headerValuePair.split(":", 2);
    setHeader(headerValuePairSplitByColon[0], headerValuePairSplitByColon[1]);
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public String getProtocol() {
    return protocol;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public HttpStatusCode getErrorCode() {
    return errorCode;
  }

  public String getBody() {
    return body;
  }

  public void setMethod(String method) {
    if (!validateMethod(method))
      throw new HttpError(BAD_REQUEST);

    if (!Arrays.asList("GET", "POST", "HEAD").contains(method))
      throw new HttpError(NOT_IMPLEMENTED);

    this.method = method;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setProtocol(String protocol) {
    if (!validateProtocol(protocol))
      throw new HttpError(BAD_REQUEST);

    if (!Arrays.asList("HTTP/1.0", "HTTP/1.1").contains(protocol))
      throw new HttpError(HTTP_VERSION_NOT_SUPPORTED);

    this.protocol = protocol;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public void setErrorCode(HttpStatusCode errorCode) {
    this.errorCode = errorCode;
  }

  public void setBody(String body) {
    this.body = body;
  }

  @Override
  public String toString() {
    String headersString = getHeaders().entrySet().stream().map((entry) -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(CRLF));
    return getMethod() + " " + getPath() + " " + getProtocol() +
      ("".equals(headersString) ? "" : CRLF + headersString) +
      CRLF + CRLF +
      (getBody() == null ? "" : getBody());
  }
}
