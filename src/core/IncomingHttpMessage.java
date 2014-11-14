package core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static core.HttpRequestRegEx.*;
import static core.HttpStatusCode.BAD_REQUEST;
import static core.HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED;

public class IncomingHttpMessage extends HttpMessage {
  public String readStartLineAndHeaders(InputStream in) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();

    int byteRead;
    while ((byteRead = in.read()) != -1) {
      if (stringBuilder.toString().equals(CRLF))
        stringBuilder = new StringBuilder();

      stringBuilder.append(new String(new byte[]{(byte) byteRead}, StandardCharsets.ISO_8859_1));

      if ((CRLF+CRLF).equals(stringBuilder.substring(Math.max(0,stringBuilder.length()-4))))
        break;
    }

    return stringBuilder.toString();
  }

  public String readBody(InputStream in) throws IOException {
    String contentLength = getHeader("CONTENT-LENGTH");
    //TODO should return 400 error if there is body and no Content-Length header, think how to differentiate it from time-out
    if (contentLength == null)
      return null;

    int numericContentLength;
    try {
      numericContentLength = Integer.parseInt(contentLength);
    } catch (NumberFormatException e) {
      throw new HttpError(BAD_REQUEST);
    }

    if (numericContentLength == 0)
      return "";

    byte[] buffer = new byte[numericContentLength];
    int bytesActuallyRead = in.read(buffer, 0, numericContentLength);
    return new String(
      buffer, 0, bytesActuallyRead,
      getBodyCharset() == null ? StandardCharsets.ISO_8859_1 : getBodyCharset()
    );
  }

  @Override
  public void setProtocol(String protocol) {
    if (!validateProtocol(protocol))
      throw new HttpError(BAD_REQUEST);

    if (!Arrays.asList("HTTP/1.0", "HTTP/1.1").contains(protocol))
      throw new HttpError(HTTP_VERSION_NOT_SUPPORTED);

    this.protocol = protocol;
  }

  @Override
  public void setHeader(String header, String value) {
    String headerValue = getHeader(header);

    getHeaders().put(header, (headerValue == null ? "" : headerValue + ", ") + value.trim());
  }

  public void setHeader(String headerValuePair) {
    if (!validateHeader(headerValuePair))
      throw new HttpError(BAD_REQUEST);

    String[] headerValuePairSplitByColon = headerValuePair.split(":", 2);
    setHeader(headerValuePairSplitByColon[0], headerValuePairSplitByColon[1]);
  }

  public void parseAndSetHeaders(String allHeaders) {
    for (String header : replaceMultipleLWSWithSingleSpace(allHeaders).split(CRLF))
      setHeader(header);
  }
}
