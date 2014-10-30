import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Request {
  private String method;
  private String path;
  private String protocol;
  private Map<String, String> headers = new HashMap<>();

  public Request(String requestLine) {
    String[] requestLineSplitBySpace = requestLine.split(" ");
    setMethod(requestLineSplitBySpace[0]);
    setPath(requestLineSplitBySpace[1]);
    setProtocol(requestLineSplitBySpace[2]);
  }

  public String getHeader(String header) {
    return getHeaders().get(header);
  }

  public void setHeader(String header, String value) {
    getHeaders().put(header, value);
  }

  public void setHeader(String headerValuePair) {
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

  public void setMethod(String method) {
    this.method = method;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  @Override
  public String toString() {
    String headersString = getHeaders().entrySet().stream().map((entry) -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining("\r\n"));
    return getMethod() + " " + getPath() + " " + getProtocol() + "\r\n" + headersString;
  }
}
