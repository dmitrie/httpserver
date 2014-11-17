package core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static core.HttpRequestRegEx.*;
import static core.HttpStatusCode.*;

public class Request extends IncomingHttpMessage {
  private String method;
  private URI requestURI;
  private Map<String, List<String>> parameters;

  public Request(ServerConfiguration serverConfiguration) {
    setServerConfiguration(serverConfiguration);
  }

  public Request(InputStream in, ServerConfiguration serverConfiguration) throws IOException {
    setServerConfiguration(serverConfiguration);
    try {
      String[] requestLineAndHeaders = readStartLineAndHeaders(in).split(CRLF,2);

      if (requestLineAndHeaders.length == 2)
        parseAndSetHeaders(requestLineAndHeaders[1]);

      if (getHeader("TRANSFER-ENCODING") != null)
        throw new HttpError(NOT_IMPLEMENTED);

      setRequestLineMembers(requestLineAndHeaders[0]);
      setBodyRelatedFields(in);
    } catch (HttpError e) {
     setResponseStatusCode(e.getErrorCode());
    }
  }

  public void setBodyRelatedFields(InputStream in) throws IOException {
    Charset charsetInHeaders = getParsedBodyCharset(getHeader("Content-Type"));
    setBodyCharset(charsetInHeaders == null ? StandardCharsets.ISO_8859_1 : charsetInHeaders);
    setBody(readBody(in));
    parseBody();
  }

  public void parseBody() throws UnsupportedEncodingException {
    validateContentHeaders();

    if (getBody() == null)
      return;

    if (!getHeader("Content-Length").equals(getContentLength()))
      throw new HttpError(BAD_REQUEST);

    String contentType = getHeader("Content-Type");
    if (contentType != null) {
      if (contentType.matches(".*multipart/form-data.*"))
        throw new HttpError(NOT_IMPLEMENTED);

      if (contentType.matches(".*application/x-www-form-urlencoded.*"))
        parseParameters(getBody());
    }
  }

  public void setRequestLineMembers(String requestLine) {
    if (!validateRequestLineFormat(requestLine))
      throw new HttpError(BAD_REQUEST);

    String[] splitRequestLine = requestLine.split(" ");

    setMethod(splitRequestLine[0]);
    setRequestURI(splitRequestLine[1]);
    setHttpVersion(splitRequestLine[2]);

    setStartLine(getMethod() + " " +
                 getParsedRequestURIAsRequested(splitRequestLine[1]) + " " +
                 getHttpVersion());
  }

  public String getParsedRequestURIAsRequested(String rawURI) {
    if (rawURI.equals("*"))
      return "*";
    else if (rawURI.charAt(0) == '/')
      return getRequestURI().getPath();
    else
      return getRequestURI().toString();
  }

  public void setMethod(String method) {
    if (!validateMethod(method))
      throw new HttpError(BAD_REQUEST);

    if (!getServerConfiguration().getImplementedMethods().contains(method))
      throw new HttpError(NOT_IMPLEMENTED);

    this.method = method;
  }

  public void setRequestURI(String uriString) {
    throwHttpErrorIfUriIsNotValid(uriString);

    try {
      if (uriString.equals("*")) {
        this.requestURI = null;
      } else {
        URI uri = new URI((getHeader("HOST") == null? "" : "http://" + getHeader("HOST")) + uriString);
        setParameters(uri);
        this.requestURI = uri;
      }
    } catch (MalformedURLException | URISyntaxException e) {
      throw new HttpError(BAD_REQUEST);
    }
  }

  public void setParameters(URI uri) throws MalformedURLException {
    URL url = uri.toURL();
    if (url.getQuery() != null)
      parseParameters(url.getQuery());
  }

  public void parseParameters(String parameters) {
    setQueryParameters(new LinkedHashMap<>());
    String[] splitParameters = parameters.split("&");
    for (String parameter : splitParameters) {
      String[] splitParameter = parameter.split("=", 2);
      decodeAndSetParameterKeyValuePairs(splitParameter);
    }
  }

  public void decodeAndSetParameterKeyValuePairs(String[] splitParameter) {
    try {
      String key = URLDecoder.decode(splitParameter[0], StandardCharsets.UTF_8.name());
      List<String> value = this.parameters.getOrDefault(key, new LinkedList<>());

      if (splitParameter.length == 2)
        value.add(URLDecoder.decode(splitParameter[1], StandardCharsets.UTF_8.name()));
      else
        value.add(null);

      this.parameters.put(key, value);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void throwHttpErrorIfUriIsNotValid(String uri) {
    if (uri.indexOf("/")!=0 && uri.indexOf("http")!=0 && !uri.equals("*"))
      throw new HttpError(BAD_REQUEST);

    if (getHeader("HOST") == null && uri.charAt(0) == '/')
      throw new HttpError(BAD_REQUEST);

    if (uri.length() > getServerConfiguration().getMaximumURILength())
      throw new HttpError(REQUEST_URI_TOO_LONG);

    if (uri.indexOf("http")==0 && !getServerConfiguration().isAbsoluteUriIsAllowed())
      throw new HttpError(BAD_REQUEST);

    if(uri.equals("*") && !getMethod().equals("OPTIONS"))
      throw new HttpError(BAD_REQUEST);
  }

  public String getMethod() {
    return method;
  }

  public URI getRequestURI() {
    return requestURI;
  }

  public Map<String, List<String>> getParameters() {
    return parameters;
  }

  public void setRequestURI(URI requestURI) {
    this.requestURI = requestURI;
  }

  public void setQueryParameters(Map<String, List<String>> queryParameters) {
    this.parameters = queryParameters;
  }
}
