package core;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static core.HttpStatusCode.REQUEST_TIMEOUT;

public class HttpMessageReader {

  private static final byte[] NEWLINE = {(byte) 13, (byte) 10};

  public static String readExactNumberOfBytes(InputStream in, int contentLength, Charset charset) {
    byte[] buffer = new byte[contentLength];
    int bytesActuallyRead;
    try {
      bytesActuallyRead = in.read(buffer, 0, contentLength);
    } catch (SocketTimeoutException e) {
      throw new HttpError(REQUEST_TIMEOUT);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read body from input stream");
    }
    return new String(buffer, 0, bytesActuallyRead, charset);
  }

  public static String readStartLineAndHeaders(InputStream in) {
    byte[] bytes = new byte[0];
    int byteRead;
    try {
      while ((byteRead = in.read()) != -1) {
        bytes = Arrays.copyOf(bytes, bytes.length + 1);
        bytes[bytes.length - 1] = (byte) byteRead;
        bytes = Arrays.equals(bytes, NEWLINE) ? new byte[0] : bytes;
        if (isEndOfHeaders(bytes)) break;
      }
    } catch (SocketTimeoutException e) {
      throw new HttpError(REQUEST_TIMEOUT);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read start-line and headers from input stream");
    }

    if (bytes.length >= 4)
      bytes = Arrays.copyOfRange(bytes, 0, bytes.length-4);
    return new String(bytes, StandardCharsets.ISO_8859_1);
  }

  private static boolean isEndOfHeaders(byte[] bytes) {
    int size = bytes.length;
    return size>=4 &&
      NEWLINE[0] == bytes[size-4] &&
      NEWLINE[1] == bytes[size-3] &&
      NEWLINE[0] == bytes[size-2] &&
      NEWLINE[1] == bytes[size-1];
  }
}
