package core;

import util.HttpStatusCode;

import static util.Helper.getServerTime;

public class Response extends HttpMessage {
  private String requestMethod;

  public Response(Request request) {
    setRequestFieldsNeededInResponse(request);
    if (request.getResponseStatusCode() != null)
      setStandardResponse(request.getResponseStatusCode());
  }

  public void setRequestFieldsNeededInResponse(Request request) {
    setProtocol(request.getProtocol() == null ? "HTTP/1.1" : request.getProtocol());
    this.requestMethod = request.getMethod();
  }

  public void setStandardResponse(HttpStatusCode code) {
    setResponseStatusCode(code);
    setBody(code.toString());
    setHeader("Content-Type", "text/html; charset=" + getBodyCharset());
    setHeader("Last-modified", getServerTime());
  }

  @Override
  public String generateMessage() {
    finalizeResponse();
    return super.generateMessage();
  }

  public void finalizeResponse() {
    setStartLine(getProtocol() + " " + getResponseStatusCode());

    if (getBody() != null)
      setHeader("Content-Length", getContentLength());

    if ("HEAD".equals(requestMethod))
      setBody(null);
  }
}
