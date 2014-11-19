package core;

import static core.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static util.Helper.getServerTime;

public class ResponseOld extends HttpMessageOld {
  private String requestMethod;

  public ResponseOld(Request request) {
    setRequestFieldsNeededInResponse(request);
    if (request.responseStatusCode != null)
      setStandardResponse(request.responseStatusCode);
  }

  public void setRequestFieldsNeededInResponse(Request request) {
    setHttpVersion(request.httpVersion == null ? "HTTP/1.1" : request.httpVersion);
    this.requestMethod = request.method;
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
    setStartLine(getHttpVersion() + " " + getResponseStatusCode());

    if (getBody() == null)
      getHeaders().remove("Content-Length");
    else
      setHeader("Content-Length", getContentLength());

    if ("HEAD".equals(requestMethod))
      setBody(null);
    else
      try {
        validateContentHeaders();
      } catch (HttpError e) {
        setStandardResponse(INTERNAL_SERVER_ERROR);
      }
  }
}
