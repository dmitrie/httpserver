package core;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import static core.HttpMessageReader.readExactNumberOfBytes;
import static core.HttpMessageReader.readStartLineAndHeaders;
import static core.HttpRequestRegEx.*;
import static core.HttpStatusCode.*;

public class RequestParser {
  Request request;
  Configuration configuration;

  public RequestParser(Configuration configuration) {
    this.configuration = configuration;
    this.request = new Request();
  }

  public Request setFields(InputStream in) {
    try {
      parse(in);
    } catch (HttpError e) {
      request.responseStatusCode = e.getErrorCode();
    }
    return request;
  }

  public void parse(InputStream in) {
    String[] requestLineAndHeaders = readStartLineAndHeaders(in).split(CRLF,2);
    String requestLine = requestLineAndHeaders[0];
    String headers = null;
    if (requestLineAndHeaders.length == 2)
      headers = requestLineAndHeaders[1];

    parseRequestLine(requestLine);
    parseHeaders(headers);
    request.requestURI = buildAbsoluteURI(request.getHeader("Host"), request.requestURI);
    parseParameters();

    setBodyCharset();
    readBody(in);
    parseBody();

    validateHeaders();
  }

  public void setBodyCharset() {
    Charset parsedBodyCharset = getParsedBodyCharset(request.getHeader("Content-Type"));
    if (parsedBodyCharset != null)
      request.bodyCharset = parsedBodyCharset;
  }

  public void parseRequestLine(String requestLine) {
    if (!validateRequestLineFormat(requestLine))
      throw new HttpError(BAD_REQUEST);
    String[] splitRequestLine = requestLine.split(" ");

    setMethod(splitRequestLine[0]);
    setRequestURI(splitRequestLine[1]);
    setHttpVersion(splitRequestLine[2]);
  }

  public static URI buildAbsoluteURI(String host, URI path) {
    if (host != null) {
      try {
        return new URI("http://" + host).resolve(path);
      } catch (URISyntaxException e) {
        throw new HttpError(BAD_REQUEST);
      }
    } else
      throw new HttpError(BAD_REQUEST);
  }

  public void parseHeaders(String multipleHeaders) {
    if (multipleHeaders == null)
      return;

    for (String headerLine : replaceMultipleLWSWithSingleSpace(multipleHeaders).split(CRLF))
      setHeader(headerLine);
  }

  public void setHeader(String headerLine) {
    if (!validateHeader(headerLine))
      throw new HttpError(BAD_REQUEST);

    String[] headerAndValue = headerLine.split(":", 2);
    String header = headerAndValue[0];
    String value = headerAndValue[1].trim();

    String existingValue = request.getHeader(header);
    if (existingValue != null)
      value = existingValue + ", " + value;

    request.setHeader(header, value);
  }

  public void setHttpVersion(String httpVersion) {
    request.httpVersion = httpVersion;

    if (!validateHttpVersion(request.httpVersion))
      throw new HttpError(BAD_REQUEST);

    if (!configuration.getSupportedHttpVersions().contains(request.httpVersion))
      throw new HttpError(HTTP_VERSION_NOT_SUPPORTED);
  }

  public void setRequestURI(String uri) {
    if (uri.indexOf("http://")==0 || "*".equals(uri))
      throw new HttpError(NOT_IMPLEMENTED);

    if (uri.charAt(0) != '/')
      throw new HttpError(BAD_REQUEST);

    if (uri.length() > configuration.getMaximumURILength())
      throw new HttpError(REQUEST_URI_TOO_LONG);

    try {
      request.requestURI = new URI(uri);
    } catch (URISyntaxException e) {
      throw new HttpError(BAD_REQUEST);
    }
  }

  public void setMethod(String method) {
    request.requestMethod = method;

    if (!validateMethod(request.requestMethod))
      throw new HttpError(BAD_REQUEST);

    if (!configuration.getImplementedMethods().contains(method))
      throw new HttpError(NOT_IMPLEMENTED);
  }

  public void parseParameters() {
    try {
      URL url = request.requestURI.toURL();
      if (url.getQuery() != null && "GET".equals(request.requestMethod))
        splitParameters(url.getQuery());
    } catch (MalformedURLException e) {
      throw new HttpError(BAD_REQUEST);
    }
  }

  public void splitParameters(String parameters) {
    if (parameters.isEmpty()) return;

    String[] splitParameters = parameters.split("&");
    for (String parameter : splitParameters) {
      String[] splitParameter = parameter.split("=", 2);

      String name = splitParameter[0];
      String value = null;
      if (splitParameter.length == 2)
        value = splitParameter[1];

      decodeAndSetParameters(name, value);
    }
  }

  public void decodeAndSetParameters(String name, String value) {
    try {
      name = URLDecoder.decode(name, StandardCharsets.UTF_8.name());
      LinkedList<String> values = request.parameters.getOrDefault(name, new LinkedList<>());
      values.add(value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
      request.parameters.put(name, values);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void parseBody() {
    if (request.body == null || !"POST".equals(request.requestMethod)) return;

    String contentType = request.getHeader("Content-Type");
    if (contentType != null) {
      if (contentType.matches(".*multipart/form-data.*"))
        throw new HttpError(NOT_IMPLEMENTED);

      if (contentType.matches(".*application/x-www-form-urlencoded.*"))
        splitParameters(request.body);
    }
  }

  public void validateHeaders() {
    if (!request.contentHeadersAreCorrect())
      throw new HttpError(BAD_REQUEST);

    if (request.getHeader("Transfer-Encoding") != null)
      throw new HttpError(NOT_IMPLEMENTED);
  }

  public void readBody(InputStream in) {
    String contentLength = request.getHeader("Content-Length");

    if (contentLength == null) {
      request.body = null;
      return;
    }
    if ("0".equals(contentLength)) {
      request.body = "";
      return;
    }

    int numericContentLength = parseContentLengthHeader(contentLength);
    request.body = readExactNumberOfBytes(in, numericContentLength, request.bodyCharset);

    if (request.calculateContentLength() != numericContentLength)
      throw new HttpError(BAD_REQUEST);
  }

  public int parseContentLengthHeader(String contentLength) {
    try {
      return Integer.parseInt(contentLength);
    } catch (NumberFormatException e) {
      throw new HttpError(BAD_REQUEST);
    }
  }

}
