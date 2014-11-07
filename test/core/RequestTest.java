package core;

import org.junit.Test;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;

public class RequestTest {

  @Test
  public void testConstructorUseLFInsteadOfCRLF_RFC2616_2_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\n" +
                           "Cache-Control: no-cache\n\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testConstructorInvalidProtocol_RFC2616_3_1() throws Exception {
    String requestString = "GET / HTTP1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testConstructorSuccessRequestLineOnly_RFC2616_4_1() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("GET", request.getMethod());
    assertEquals("http://google.com", request.getPath());
    assertEquals("HTTP/1.1", request.getProtocol());

    assertEquals(requestString, request.toString());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorSuccessRequestLineAndHeaders_RFC2616_4_1() throws Exception {
    String requestString = "HEAD /test.html HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
                           "Cache-Control: no-store\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("HEAD", request.getMethod());
    assertEquals("/test.html", request.getPath());
    assertEquals("HTTP/1.0", request.getProtocol());
    assertEquals("www.google.com", request.getHeader("HOST"));
    assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", request.getHeader("ACCEPT"));
    assertEquals("no-store", request.getHeader("CACHE-CONTROL"));

    assertEquals(requestString, request.toString());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorSuccessRequestLineAndHeadersAndBody_RFC2616_4_1() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Content-length: 24\r\n\r\n" +
                           "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("POST", request.getMethod());
    assertEquals("/test", request.getPath());
    assertEquals("HTTP/1.0", request.getProtocol());
    assertEquals("24", request.getHeader("CONTENT-LENGTH"));
    assertEquals("\r\nSome  \r\n  body  here\r\n", request.getBody());

    assertEquals(requestString, request.toString());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorIgnoresLeadingCRLFInInputStream_RFC2616_4_1() throws Exception {
    String requestString = "\r\n\r\nGET http://www.google.com HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("GET", request.getMethod());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorEmptyLineBetweenHeadersIsTreatedAsEndOfHeaders_RFC2616_4_1() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache-Control: no-cache\r\n" +
                           "Content-Length: 5\r\n" +
                           "\r\n" +
                           "Accept-Encoding: gzip, deflate\r\n\r\n"+
                           "abcde";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("no-cache", request.getHeader("CACHE-CONTROL"));
    assertEquals(null, request.getHeader("ACCEPT-ENCODING"));
    assertEquals("Accep", request.getBody());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorReplaceMultipleLWSWithSingleSpace_RFC2616_4_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache-Control: \r\n\t   \r\n\t n o \r\n -cac\t\the\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("n o -cac he", request.getHeader("CACHE-CONTROL"));
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorNoSpacesAllowedInHeaderName_RFC2616_4_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache- Control: test";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testInvalidHeader_RFC2616_4_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache-Control no-cache\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testHeaderValuesAreTrimmed_RFC2616_4_2() throws Exception {
    String requestString = "GET / HTTP/1.1\r\n" +
                           "Host: \r\n\t   www.google.com \r\n \r\n\t  \t\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("www.google.com", request.getHeader("HOST"));
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testHeaderNamesAreCaseInsensitive_RFC2616_4_2() throws Exception {
    Request request = new Request();
    request.setHeaders(new LinkedCaseInsensitiveMap<String>(){{
      put("Cache-Control", "no-cache");
    }});

    assertEquals("no-cache", request.getHeader("CACHe-Control"));
    request.setHeader("CACHE-controL", "no-store");
    assertEquals("no-cache, no-store", request.getHeader("cache-Control"));
  }

  @Test
  public void testConstructorMultipleHeadersAreMerged_RFC2616_4_2() throws Exception {
    String requestString = "HEAD http://google.com/test.html HTTP/1.0\r\n" +
                           "Cache-Control: no-cache\r\n" +
                           "Cache-Control: no-store\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("HEAD", request.getMethod());
    assertEquals("http://google.com/test.html", request.getPath());
    assertEquals("HTTP/1.0", request.getProtocol());
    assertEquals("no-cache, no-store", request.getHeader("CACHE-CONTROL"));

    assertEquals("HEAD http://google.com/test.html HTTP/1.0\r\n" +
                 "Cache-Control: no-cache, no-store\r\n\r\n", request.toString());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorContentLengthIsSmallerThanBodyLength_RFC2616_4_4() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Content-length: 10\r\n\r\n" +
                           "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("POST", request.getMethod());
    assertEquals("/test", request.getPath());
    assertEquals("HTTP/1.0", request.getProtocol());
    assertEquals("10", request.getHeader("CONTENT-LENGTH"));
    assertEquals("\r\nSome  \r\n", request.getBody());

    assertEquals("POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 10\r\n\r\n" +
      "\r\nSome  \r\n", request.toString());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorZeroContentLength_RFC2616_4_4() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Content-length: 0\r\n\r\n" +
                           "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals("POST", request.getMethod());
    assertEquals("/test", request.getPath());
    assertEquals("HTTP/1.0", request.getProtocol());
    assertEquals("0", request.getHeader("CONTENT-LENGTH"));
    assertEquals("", request.getBody());

    assertEquals("POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 0\r\n\r\n", request.toString());
    assertEquals(null, request.getErrorCode());
  }

  @Test
  public void testConstructorInvalidRequestLine_RFC2616_5_1() throws Exception {
    String requestString = "GET /some file name with spaces.html HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testConstructorInvalidMethod_RFC2616_5_1_1() throws Exception {
    String requestString = "GE/T /test HTTP/1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testConstructorUnsupportedMethod_RFC2616_5_1_1() throws Exception {
    String requestString = "DELETE /test HTTP/1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(NOT_IMPLEMENTED, request.getErrorCode());
  }

  @Test
  public void testConstructorUnsupportedExtensionMethod_RFC2616_5_1_1() throws Exception {
    String requestString = "OTHER_METHOD /test HTTP/1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(NOT_IMPLEMENTED, request.getErrorCode());
  }

  @Test
  public void testConstructorNoHostHeaderWithAbsPath_RFC2616_5_1_2() throws Exception {
    String requestString = "GET / HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);

    assertEquals(BAD_REQUEST, request.getErrorCode());
  }

  @Test
  public void testConstructorUnsupportedProtocol_RFC2616_10_5_6() throws Exception {
    String requestString = "GET / HTTP/0.9\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(HTTP_VERSION_NOT_SUPPORTED, request.getErrorCode());
  }

  @Test
  public void testConstructorTransferEncodingHeaderIsNotSupported() throws Exception {
    String requestString = "GET / HTTP/1.1\r\n" +
                           "Transfer-Encoding: foo\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in);
    assertEquals(NOT_IMPLEMENTED, request.getErrorCode());
  }

  @Test
  public void testHeaderNamesCaseIsPreserved() throws Exception {
    Request request = new Request();
    request.setHeader("Cache-Control", "no-cache");
    assertEquals("Cache-Control", request.getHeaders().keySet().iterator().next());
  }
}