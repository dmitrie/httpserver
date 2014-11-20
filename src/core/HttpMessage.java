package core;

import util.LinkedCaseInsensitiveMap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class HttpMessage {
  public String requestMethod;
  public String httpVersion;
  public Map<String, String> headers = new LinkedCaseInsensitiveMap();
  String body;
  public Charset bodyCharset = StandardCharsets.ISO_8859_1;
  public HttpStatusCode responseStatusCode;

  public String getHeader(String header) {
    return headers.get(header);
  }

  public void setHeader(String header, String value) {
    this.headers.put(header, value);
  }

  public int calculateContentLength() {
    return body == null ? 0 : body.getBytes(bodyCharset).length;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public boolean contentHeadersAreCorrect() {
    if (body == null && !"HEAD".equals(requestMethod))
      for(String key : headers.keySet())
        if (Pattern.compile("Content-.*", Pattern.CASE_INSENSITIVE).matcher(key).matches())
          return false;

    return true;
  }
}
