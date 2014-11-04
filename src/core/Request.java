package core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static core.HttpStatusCode.BAD_REQUEST;
import static core.HttpStatusCode.NOT_IMPLEMENTED;

public class Request {
  private String method;
  private String path;
  private String protocol = "HTTP/1.1";
  private Map<String, String> headers = new LinkedHashMap<>();
  private HttpStatusCode errorCode;
  private String body;

  public Request() {}

  public Request(InputStream in) throws IOException {
    try {
      String[] requestLineAndHeaders = readRequestLineAndHeaders(in).split("\r\n");
      setRequestLineMembers(requestLineAndHeaders[0]);

      if (requestLineAndHeaders.length > 1)
        for (int i = 1; i < requestLineAndHeaders.length; i++)
          setHeader(requestLineAndHeaders[i]);

      if (getHeader("TRANSFER-ENCODING") != null)
        throw new HttpError(NOT_IMPLEMENTED);

      setBody(readBody(in));
    } catch (HttpError e) {
     setErrorCode(e.getErrorCode());
    }
  }

  public String readRequestLineAndHeaders(InputStream in) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();

    int byteRead;
    while ((byteRead = in.read()) != -1) {
      stringBuilder.append((char) byteRead);
      if ("\r\n\r\n".equals(stringBuilder.substring(Math.max(0,stringBuilder.length()-4))))
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
    in.read(buffer, 0, numericContentLength);
    return new String(buffer);
  }

  public void setRequestLineMembers(String requestLine) {
    String[] splitRequestLine = requestLine.split(" ");

    if (splitRequestLine.length != 3)
      throw new HttpError(BAD_REQUEST);

    setMethod(splitRequestLine[0]);
    setPath(splitRequestLine[1]);
    setProtocol(splitRequestLine[2]);
  }

  public String getHeader(String header) {
    return getHeaders().get(header);
  }

  public void setHeader(String header, String value) {
    header = header.trim().toUpperCase();
    String headerValue = getHeader(header);

    getHeaders().put(header, (headerValue == null ? "" : headerValue + ", ") + value.trim());
  }

  public void setHeader(String headerValuePair) {
    String[] headerValuePairSplitByColon = headerValuePair.split(":", 2);
    if (headerValuePairSplitByColon.length != 2)
      throw new HttpError(BAD_REQUEST);

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
    if (!Arrays.asList("OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT").contains(method))
      throw new HttpError(BAD_REQUEST);

    if (!Arrays.asList("GET", "POST", "HEAD").contains(method))
      throw new HttpError(NOT_IMPLEMENTED);

    this.method = method;
  }

  public void setPath(String path) {
    if ("".equals(path))
      throw new HttpError(BAD_REQUEST);

    this.path = path;
  }

  public void setProtocol(String protocol) {
    if (!protocol.matches("HTTP/\\d\\.\\d"))
      throw new HttpError(BAD_REQUEST);

    if (!Arrays.asList("HTTP/1.0", "HTTP/1.1").contains(protocol))
      throw new HttpError(NOT_IMPLEMENTED);

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
    String headersString = getHeaders().entrySet().stream().map((entry) -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("\r\n"));
    return getMethod() + " " + getPath() + " " + getProtocol() +
      ("".equals(headersString) ? "" : "\r\n" + headersString) +
      "\r\n\r\n" +
      (getBody() == null ? "" : getBody());
  }
}
