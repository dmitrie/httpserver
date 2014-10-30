import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Response {
  private String protocol;
  private String responseCode;
  private Map<String, String> headers = new HashMap<>();
  private String body;
  private String encoding = "UTF-8";

  public Response(String protocol) throws UnsupportedOperationException {
    if (!protocol.equals("HTTP/1.1"))
      throw new UnsupportedOperationException("Server supports only HTTP/1.1 protocol");
    this.protocol = protocol;
  }

  public void setError(int code) throws UnsupportedEncodingException {
    switch (code) {
      case 404:
        setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
        setResponseCode("404 Not Found");
        break;
      case 500:
        setBody("500 Internal Server Error");
        setResponseCode("500 Internal Server Error");
        break;
      default:
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
    getHeaders().put(header, value);
  }

  public String getProtocol() {
    return protocol;
  }

  public String getResponseCode() {
    return responseCode;
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

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public void setResponseCode(String responseCode) {
    this.responseCode = responseCode;
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

  @Override
  public String toString() {
    String headersString = getHeaders().entrySet().stream().map((entry) -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining("\r\n"));
    return getProtocol() + " " + getResponseCode() + "\r\n" + headersString + "\r\n\r\n" + getBody();
  }
}
