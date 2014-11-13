package core;

public class Response extends HttpMessage {
  private Request request;

  public Response(Request request) {
    setRequest(request);
    setBodyCharset(request.getDefaultCharset());
    setProtocol(request.getProtocol() == null ? "HTTP/1.1" : request.getProtocol());
    if (request.getResponseStatusCode() != null)
      setErrorBodyAndHeaders(request.getResponseStatusCode());
  }

  @Override
  public void setHeader(String header, String value) {
    getHeaders().put(header, value);
  }

  public void setErrorBodyAndHeaders(HttpStatusCode code) {
    setResponseStatusCode(code);

    switch (code.getCode()) {
      case 404:
        setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
        break;
      default:
        setBody(code.toString());
        break;
    }

    setHeader("Content-Type", "text/html; charset=" + getBodyCharset());
    setHeader("Last-modified", Server.getServerTime());
  }

  @Override
  public String generateMessage() {
    setStartLine(getProtocol() + " " + getResponseStatusCode());
    if ("HEAD".equals(getRequest().getMethod()))
      setBody(null);

    if (getBody() != null)
      setHeader("Content-Length", getContentLength());

    return super.generateMessage();
  }

  public Request getRequest() {
    return request;
  }

  public void setRequest(Request request) {
    this.request = request;
  }
}
