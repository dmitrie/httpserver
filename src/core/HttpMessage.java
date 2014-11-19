package core;

import util.LinkedCaseInsensitiveMap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class HttpMessage {
  public String httpVersion;
  public Map<String, String> headers = new LinkedCaseInsensitiveMap();
  public String body;
  public Charset bodyCharset = StandardCharsets.ISO_8859_1;
  public HttpStatusCode responseStatusCode;

  public String getHeader(String header) {
    return headers.get(header);
  }

  public void setHeader(String header, String value) {
    this.headers.put(header, value);
  }

  public int getContentLength() {
    if (body == null)
      return 0;
    else
      return body.getBytes(bodyCharset).length;
  }
}
