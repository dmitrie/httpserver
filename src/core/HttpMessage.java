package core;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static core.HttpRequestRegEx.CRLF;

public class HttpMessage {
  protected String protocol;
  protected String startLine;
  protected Map<String, String> headers = new LinkedCaseInsensitiveMap();
  protected String body;
  protected Charset bodyEncoding = StandardCharsets.ISO_8859_1;
  protected HttpStatusCode responseStatusCode;
  protected ServerConfiguration serverConfiguration;

  public String getHeader(String header) {
    return getHeaders().get(header);
  }

  public void setHeader(String header, String value) {
    getHeaders().put(header, value);
  }

  public String generateMessage() {
    String headersString = getHeaders().entrySet().stream().map((entry) -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(CRLF));
    String message = "";
    message = getStartLine() +
      ("".equals(headersString) ? "" : CRLF + headersString) +
      CRLF + CRLF +
      (getBody() == null ? "" : getBody());
    return message;
  }

  public String getContentLength() {
    return "" + getBody().getBytes(getBodyEncoding()).length;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getStartLine() {
    return startLine;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getBody() {
    return body;
  }

  public Charset getBodyEncoding() {
    return bodyEncoding;
  }

  public HttpStatusCode getResponseStatusCode() {
    return responseStatusCode;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public void setStartLine(String startLine) {
    this.startLine = startLine;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setBodyEncoding(Charset bodyEncoding) {
    this.bodyEncoding = bodyEncoding;
  }

  public void setResponseStatusCode(HttpStatusCode responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }
}
