package core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static core.HttpRequestRegEx.*;
import static core.HttpStatusCode.*;
import static core.Server.combinePaths;

public class Request extends IncomingHttpMessage {
  private String method;
  private URI requestURI;
  private String localPath;
  private Map<String, List<String>> queryParameters;

  public Request(ServerConfiguration serverConfiguration) {
    this.serverConfiguration = serverConfiguration;
  }

  public Request(InputStream in, ServerConfiguration serverConfiguration) throws IOException {
    this.serverConfiguration = serverConfiguration;
    try {
      String[] requestLineAndHeaders = readStartLineAndHeaders(in).split(CRLF,2);

      if (requestLineAndHeaders.length == 2) {
        parseAndSetHeaders(requestLineAndHeaders[1]);
      }

      if (getHeader("TRANSFER-ENCODING") != null)
        throw new HttpError(NOT_IMPLEMENTED);

      setRequestLineMembers(requestLineAndHeaders[0]);

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

    setStartLine(getMethod() + " " +
                 getParsedRequestURIAsRequested(splitRequestLine[1]) + " " +
                 getProtocol());
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

    if (!serverConfiguration.getImplementedMethods().contains(method))
      throw new HttpError(NOT_IMPLEMENTED);

    this.method = method;
  }

  public void setRequestURI(String uriString) {
    throwHttpErrorIfUriIsNotValid(uriString);

    try {
      if (uriString.equals("*")) {
        this.requestURI = null;
        this.localPath = null;
      } else {
        URI uri = new URI((getHeader("HOST") == null? "" : "http://" + getHeader("HOST")) + uriString);
        setQueryParameters(uri);
        this.requestURI = uri;
        this.localPath = combinePaths(getServerConfiguration().getDocumentRootPath(), uri.getPath());
      }
    } catch (MalformedURLException | URISyntaxException e) {
      throw new HttpError(BAD_REQUEST);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void setQueryParameters(URI uri) throws UnsupportedEncodingException, MalformedURLException {
    URL url = uri.toURL();
    setQueryParameters(new LinkedHashMap<String, List<String>>());
    if (url.getQuery() != null) {
      String[] parameters = url.getQuery().split("&");
      for (String parameter : parameters) {
        String[] splitParameter = parameter.split("=", 2);

        String key = URLDecoder.decode(splitParameter[0], serverConfiguration.getDefaultCharset().name());
        List<String> value = queryParameters.getOrDefault(key, new LinkedList<String>());
        if (splitParameter.length == 2)
          value.add(URLDecoder.decode(splitParameter[1], serverConfiguration.getDefaultCharset().name()));
        else
          value.add(null);

        queryParameters.put(key, value);
      }
    }
  }

  public void throwHttpErrorIfUriIsNotValid(String uri) {
    if (uri.indexOf("/")!=0 && uri.indexOf("http")!=0 && !uri.equals("*"))
      throw new HttpError(BAD_REQUEST);

    if (getHeader("HOST") == null && uri.charAt(0) == '/')
      throw new HttpError(BAD_REQUEST);

    if (uri.length() > serverConfiguration.getMaximumURILength())
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

  public Map<String, List<String>> getQueryParameters() {
    return queryParameters;
  }

  public String getLocalPath() {
    return localPath;
  }

  public void setRequestURI(URI requestURI) {
    this.requestURI = requestURI;
  }

  public void setQueryParameters(Map<String, List<String>> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public void setLocalPath(String localPath) {
    this.localPath = localPath;
  }
}
