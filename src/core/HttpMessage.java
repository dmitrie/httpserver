package core;

import util.LinkedCaseInsensitiveMap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static core.HttpRequestRegEx.CRLF;
import static core.HttpStatusCode.BAD_REQUEST;
import static util.StringUtils.addPostfix;
import static util.StringUtils.defaultString;

public abstract class HttpMessage {
  private String httpVersion;
  private String startLine;
  private Map<String, String> headers = new LinkedCaseInsensitiveMap();
  private String body;
  private Charset bodyCharset = StandardCharsets.ISO_8859_1;
  private HttpStatusCode responseStatusCode;
  private Configuration configuration;

  public String generateMessage() {
    String headersString = getHeaders().entrySet().stream()
      .map((entry) -> entry.getKey() + ": " + entry.getValue())
      .collect(Collectors.joining(CRLF));

    return startLine + CRLF +
      addPostfix(headersString, CRLF) +
      CRLF +
      defaultString(body);
  }

  public String getContentLength() {
    return "" + getBody().getBytes(getBodyCharset()).length;
  }

  public String getHeader(String header) {
    return getHeaders().get(header);
  }

  public void setHeader(String header, String value) {
    getHeaders().put(header, value);
  }

  public void validateContentHeaders() {
    if (getBody() == null)
      for(String key : getHeaders().keySet())
        if (Pattern.compile("Content-.*", Pattern.CASE_INSENSITIVE).matcher(key).matches())
          throw new HttpError(BAD_REQUEST);
  }

  public String getHttpVersion() {
    return httpVersion;
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

  public Charset getBodyCharset() {
    return bodyCharset;
  }

  public HttpStatusCode getResponseStatusCode() {
    return responseStatusCode;
  }

  public void setHttpVersion(String httpVersion) {
    this.httpVersion = httpVersion;
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

  void setBodyCharset(Charset bodyCharset) {
    this.bodyCharset = bodyCharset;
  }

  public void setResponseStatusCode(HttpStatusCode responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }

  protected Configuration getConfiguration() {
    return configuration;
  }

  protected void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
