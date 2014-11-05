package core;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Response {
  public static final String CRLF = "\r\n";
  private Request request;
  private HttpStatusCode statusCode;
  private Map<String, String> headers = new LinkedHashMap<>();
  private String body;
  private String encoding = "ISO-8859-1";

  public Response(Request request) throws UnsupportedOperationException, UnsupportedEncodingException {
    this.request = request;
    if (request.getErrorCode() != null)
      setErrorBodyAndHeaders(request.getErrorCode());
  }

  public void setErrorBodyAndHeaders(HttpStatusCode code) throws UnsupportedEncodingException {
    setStatusCode(code);

    switch (code.getCode()) {
      case 404:
        setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
        break;
      default:
        setBody(code.toString());
        break;
    }

    setHeader("Content-Type", "text/html; charset=" + getEncoding());
    setHeader("Last-modified", Server.getServerTime());
    setHeader("Content-Length", "" + getContentLength());
  }

  public int getContentLength() throws UnsupportedEncodingException {
    return getBody().getBytes(getEncoding()).length;
  }

  public String getHeader(String header) {
    return getHeaders().get(header);
  }

  public void setHeader(String header, String value) {
    getHeaders().put(header.toUpperCase(), value);
  }

  public HttpStatusCode getStatusCode() {
    return statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getBody() {
    return body;
  }

  public String getEncoding() {
    return encoding;
  }

  public Request getRequest() {
    return request;
  }

  public void setStatusCode(HttpStatusCode statusCode) {
    this.statusCode = statusCode;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  @Override
  public String toString() {
    try {
      setHeader("Content-Length", "" + getContentLength());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    String headersString = getHeaders().entrySet().stream().map((entry) -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(CRLF));
    return getRequest().getProtocol() + " " + getStatusCode().toString() + CRLF + headersString + CRLF + CRLF + getBody();
  }
}
