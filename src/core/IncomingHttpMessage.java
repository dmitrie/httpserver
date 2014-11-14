package core;

import util.HttpError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static util.HttpRequestRegEx.*;
import static util.HttpStatusCode.BAD_REQUEST;
import static util.HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED;

public class IncomingHttpMessage extends HttpMessage {

  public String readStartLineAndHeaders(InputStream in) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();

    int byteRead;
    while ((byteRead = in.read()) != -1) {
      removeLeadingNewLines(stringBuilder);
      stringBuilder.append(new String(new byte[]{(byte) byteRead}, StandardCharsets.ISO_8859_1));

      if (stringBuilder.length()>=4 && (CRLF+CRLF).equals(stringBuilder.substring(stringBuilder.length()-4)))
        break;
    }

    return stringBuilder.toString();
  }

  public void removeLeadingNewLines(StringBuilder stringBuilder) {
    if (stringBuilder.toString().equals(CRLF))
      stringBuilder.delete(0, stringBuilder.length());
  }

  public String readBody(InputStream in) throws IOException {
    String contentLength = getHeader("CONTENT-LENGTH");
    //TODO should return 400 error if there is body and no Content-Length header, think how to differentiate it from time-out
    if (contentLength == null)
      return null;

    int numericContentLength = parseContentLengthHeader(contentLength);
    if (numericContentLength == 0)
      return "";
    else
      return readExactNumberOfBytes(in, numericContentLength);
  }

  public String readExactNumberOfBytes(InputStream in, int numericContentLength) throws IOException {
    byte[] buffer = new byte[numericContentLength];
    int bytesActuallyRead = in.read(buffer, 0, numericContentLength);
    return new String(buffer, 0, bytesActuallyRead, getBodyCharset());
  }

  public int parseContentLengthHeader(String contentLength) {
    try {
      int numericContentLength = Integer.parseInt(contentLength);
      return numericContentLength;
    } catch (NumberFormatException e) {
      throw new HttpError(BAD_REQUEST);
    }
  }

  @Override
  public void setProtocol(String protocol) {
    if (!validateProtocol(protocol))
      throw new HttpError(BAD_REQUEST);

    if (!serverConfiguration.getSupportedHttpVersions().contains(protocol))
      throw new HttpError(HTTP_VERSION_NOT_SUPPORTED);

    super.setProtocol(protocol);
  }

  @Override
  public void setHeader(String header, String value) {
    String existingHeaderValue = getHeader(header);
    super.setHeader(header, (existingHeaderValue == null ? "" : existingHeaderValue + ", ") + value.trim());
  }

  public void setHeader(String headerValuePair) {
    if (!validateHeader(headerValuePair))
      throw new HttpError(BAD_REQUEST);

    String[] splitHeaderValuePair = headerValuePair.split(":", 2);
    setHeader(splitHeaderValuePair[0], splitHeaderValuePair[1]);
  }

  public void parseAndSetHeaders(String multipleHeaders) {
    for (String header : replaceMultipleLWSWithSingleSpace(multipleHeaders).split(CRLF))
      setHeader(header);
  }
}
