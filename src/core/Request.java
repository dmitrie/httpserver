package core;

import java.io.IOException;
import java.io.InputStream;

import static core.HttpRequestRegEx.*;
import static core.HttpStatusCode.*;

public class Request extends IncomingHttpMessage {
  private String method;
  private String requestURI;

  public Request(ServerConfiguration serverConfiguration) {
    this.serverConfiguration = serverConfiguration;
  }

  public Request(InputStream in, ServerConfiguration serverConfiguration) throws IOException {
    this.serverConfiguration = serverConfiguration;
    try {
      String[] requestLineAndHeaders = readStartLineAndHeaders(in).split(CRLF,2);
      setRequestLineMembers(requestLineAndHeaders[0]);

      if (requestLineAndHeaders.length == 2) {
        parseAndSetHeaders(requestLineAndHeaders[1]);
      }

      if (getHeader("TRANSFER-ENCODING") != null)
        throw new HttpError(NOT_IMPLEMENTED);

      if (getHeader("HOST") == null && requestURI.charAt(0) == '/')
        throw new HttpError(BAD_REQUEST);

      setBody(readBody(in));

      if (getBody() != null && !getHeader("Content-Length").equals(getContentLength()))
         throw new HttpError(BAD_REQUEST);

    } catch (HttpError e) {
     setResponseStatusCode(e.getErrorCode());
    }
  }

  public void setRequestLineMembers(String requestLine) {
    if (!validateRequestLineFormat(requestLine))
      throw new HttpError(BAD_REQUEST);

    String[] splitRequestLine = requestLine.split(" ");

    setMethod(splitRequestLine[0]);
    setRequestURI(splitRequestLine[1]);
    setProtocol(splitRequestLine[2]);
    setStartLine(getMethod() + " " + getRequestURI() + " " + getProtocol());

    if(requestURI.equals("*") && !getMethod().equals("OPTIONS"))
      throw new HttpError(BAD_REQUEST);
  }

  public void setMethod(String method) {
    if (!validateMethod(method))
      throw new HttpError(BAD_REQUEST);

    if (!serverConfiguration.getImplementedMethods().contains(method))
      throw new HttpError(NOT_IMPLEMENTED);

    this.method = method;
  }

  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI;
    throwHttpErrorExceptionIfPathIsInvalid();
  }

  public void throwHttpErrorExceptionIfPathIsInvalid() {
    if (requestURI.length() > serverConfiguration.getMaximumURILength())
      throw new HttpError(REQUEST_URI_TOO_LONG);

    if (requestURI.indexOf("/")!=0 && requestURI.indexOf("http")!=0 && !requestURI.equals("*"))
      throw new HttpError(BAD_REQUEST);
  }

  public String getMethod() {
    return method;
  }

  public String getRequestURI() {
    return requestURI;
  }

}
