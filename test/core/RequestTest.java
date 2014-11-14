package core;

import org.junit.Before;
import org.junit.Test;
import util.HttpError;
import util.LinkedCaseInsensitiveMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static util.HttpStatusCode.*;

public class RequestTest {

  ServerConfiguration serverConfiguration = new ServerConfiguration();

  @Before
  public void setUp() throws Exception {
    serverConfiguration.setAbsoluteUriIsAllowed(true);
  }

  @Test
  public void testConstructorUseLFInsteadOfCRLF_RFC2616_2_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\n" +
                           "Cache-Control: no-cache\n\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorInvalidProtocol_RFC2616_3_1() throws Exception {
    String requestString = "GET / HTTP1.0\r\nHost: www.google.com\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorSuccessRequestLineOnly_RFC2616_4_1() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("GET", request.getMethod());
    assertEquals("http://google.com", request.getRequestURI().toString());
    assertEquals("HTTP/1.1", request.getHttpVersion());

    assertEquals(requestString, request.generateMessage());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorSuccessRequestLineAndHeaders_RFC2616_4_1() throws Exception {
    String requestString = "HEAD /test.html HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
                           "Cache-Control: no-store\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("HEAD", request.getMethod());
    assertEquals("/test.html", request.getRequestURI().getPath());
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("www.google.com", request.getHeader("HOST"));
    assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", request.getHeader("ACCEPT"));
    assertEquals("no-store", request.getHeader("CACHE-CONTROL"));

    assertEquals(requestString, request.generateMessage());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorSuccessRequestLineAndHeadersAndBody_RFC2616_4_1() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Content-length: 24\r\n\r\n" +
                           "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("POST", request.getMethod());
    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("24", request.getHeader("CONTENT-LENGTH"));
    assertEquals("\r\nSome  \r\n  body  here\r\n", request.getBody());

    assertEquals(requestString, request.generateMessage());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorIgnoresLeadingCRLFInInputStream_RFC2616_4_1() throws Exception {
    String requestString = "\r\n\r\nGET http://www.google.com HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("GET", request.getMethod());
    assertEquals(null, request.getResponseStatusCode());
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

    Request request = new Request(in, serverConfiguration);
    assertEquals("no-cache", request.getHeader("CACHE-CONTROL"));
    assertEquals("5", request.getHeader("Content-Length"));
    assertEquals(null, request.getHeader("ACCEPT-ENCODING"));

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorReplaceMultipleLWSWithSingleSpace_RFC2616_4_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache-Control: \r\n\t   \r\n\t n o \r\n -cac\t\the\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("n o -cac he", request.getHeader("CACHE-CONTROL"));
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorNoSpacesAllowedInHeaderName_RFC2616_4_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache- Control: test";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testInvalidHeader_RFC2616_4_2() throws Exception {
    String requestString = "GET http://google.com HTTP/1.1\r\n" +
                           "Cache-Control no-cache\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testHeaderValuesAreTrimmed_RFC2616_4_2() throws Exception {
    String requestString = "GET / HTTP/1.1\r\n" +
                           "Host: \r\n\t   www.google.com \r\n \r\n\t  \t\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("www.google.com", request.getHeader("HOST"));
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testHeaderNamesAreCaseInsensitive_RFC2616_4_2() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeaders(new LinkedCaseInsensitiveMap() {{ put("Cache-Control", "no-cache"); }});

    assertEquals("no-cache", request.getHeader("CACHe-Control"));
    request.setHeader("CACHE-controL", "no-store");
    assertEquals("no-cache, no-store", request.getHeader("cache-Control"));
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorMultipleHeadersAreMerged_RFC2616_4_2() throws Exception {
    String requestString = "HEAD http://google.com/test.html HTTP/1.0\r\n" +
                           "Cache-Control: no-cache\r\n" +
                           "Cache-Control: no-store\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("HEAD", request.getMethod());
    assertEquals("http://google.com/test.html", request.getRequestURI().toString());
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("no-cache, no-store", request.getHeader("CACHE-CONTROL"));

    assertEquals("HEAD http://google.com/test.html HTTP/1.0\r\n" +
                 "Cache-Control: no-cache, no-store\r\n\r\n", request.generateMessage());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorContentLengthIsSmallerThanBodyLength_RFC2616_4_4() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Content-length: 10\r\n\r\n" +
                           "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("POST", request.getMethod());
    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("www.google.com", request.getHeader("host"));
    assertEquals("10", request.getHeader("CONTENT-LENGTH"));
    assertEquals("\r\nSome  \r\n", request.getBody());

    assertEquals("POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 10\r\n\r\n" +
      "\r\nSome  \r\n", request.generateMessage());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorZeroContentLength_RFC2616_4_4() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
                           "Host: www.google.com\r\n" +
                           "Content-length: 0\r\n\r\n" +
                           "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals("POST", request.getMethod());
    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("0", request.getHeader("CONTENT-LENGTH"));
    assertEquals("", request.getBody());

    assertEquals("POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 0\r\n\r\n", request.generateMessage());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorContentLengthIsLargerThanBodyLength_RFC2616_4_4() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 1000\r\n\r\n" +
      "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorContentLengthIsNotInteger_RFC2616_4_4() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 1.1\r\n\r\n" +
      "\r\nSome  \r\n  body  here\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorInvalidRequestLine_RFC2616_5_1() throws Exception {
    String requestString = "GET /some file name with spaces.html HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorInvalidMethod_RFC2616_5_1_1() throws Exception {
    String requestString = "GE/T /test HTTP/1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorUnsupportedMethod_RFC2616_5_1_1() throws Exception {
    String requestString = "DELETE /test HTTP/1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(NOT_IMPLEMENTED, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorUnsupportedExtensionMethod_RFC2616_5_1_1() throws Exception {
    String requestString = "OTHER_METHOD /test HTTP/1.0\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(NOT_IMPLEMENTED, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorNoHostHeaderWithAbsPath_RFC2616_5_1_2() throws Exception {
    String requestString = "GET / HTTP/1.1\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);

    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorUnsupportedProtocol_RFC2616_10_5_6() throws Exception {
    String requestString = "GET http://google.com/ HTTP/0.9\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(HTTP_VERSION_NOT_SUPPORTED, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorTransferEncodingHeaderIsNotSupported() throws Exception {
    String requestString = "GET / HTTP/1.1\r\n" +
                           "Transfer-Encoding: foo\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertEquals(NOT_IMPLEMENTED, request.getResponseStatusCode());
  }

  @Test
  public void testHeaderNamesCaseIsPreserved() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("Cache-Control", "no-cache");
    assertEquals("Cache-Control", request.getHeaders().keySet().iterator().next());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestLineMembersAsteriskRequestURICorrectMethod_RFC2616_5_1_2() throws Exception {
    ServerConfiguration config = new ServerConfiguration();
    config.getImplementedMethods().add("OPTIONS");

    Request request = new Request(config);
    request.setHeader("host", "www.google.com");
    request.setRequestLineMembers("GET / HTTP/1.1");
    request.setRequestLineMembers("OPTIONS * HTTP/1.1");

    assertEquals(null, request.getRequestURI());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestLineMembersAsteriskRequestURIWrongMethod_RFC2616_5_1_2() throws Exception {
    Request request = new Request(serverConfiguration);
    try {
      request.setRequestLineMembers("GET * HTTP/1.1");
      fail();
    } catch (HttpError e) {
      assertTrue(e.getErrorCode().equals(BAD_REQUEST));
    }
  }

  @Test
  public void testSetRequestLineMembersRelativeRequestURI_RFC2616_5_1_2() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("Host", "www.google.com");
    request.setRequestLineMembers("GET /test.html HTTP/1.1");
    assertEquals("http://www.google.com/test.html", request.getRequestURI().toString());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestLineMembersAbsoluteRequestURI_RFC2616_5_1_2() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setRequestLineMembers("GET http://google.com/test.html HTTP/1.1");
    assertEquals("http://google.com/test.html", request.getRequestURI().toString());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestURITooLongURI_RFC2616_10_4_15() throws Exception {
    ServerConfiguration config = new ServerConfiguration();
    config.setMaximumURILength(10);
    Request request = new Request(config);

    request.setHeader("Host", "www.google.com");
    request.setRequestLineMembers("GET /123456789 HTTP/1.1");
    assertEquals("/123456789", request.getRequestURI().getPath());

    try {
      request.setRequestLineMembers("GET /1234567890 HTTP/1.1");
      fail();
    } catch (HttpError e) {
      assertEquals(REQUEST_URI_TOO_LONG, e.getErrorCode());
    }
  }

  @Test
  public void testSetRequestURInvalidHost() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("Host", "#$%&");

    try {
      request.setRequestLineMembers("GET /test HTTP/1.1");
      fail();
    } catch (HttpError e) {
      assertEquals(BAD_REQUEST, e.getErrorCode());
    }
  }

  @Test
  public void testSetRequestURIInvalidAbsPath() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("Host", "www.google.com");

    try {
      request.setRequestLineMembers("GET test HTTP/1.1");
      fail();
    } catch (HttpError e) {
      assertEquals(BAD_REQUEST, e.getErrorCode());
    }
  }

  @Test
  public void testSetRequestURIAbsoluteUriIsNotAllowed() throws Exception {
    ServerConfiguration config = new ServerConfiguration();
    Request request = new Request(config);

    config.setAbsoluteUriIsAllowed(true);
    request.setRequestLineMembers("GET http://www.google.com/ HTTP/1.1");
    assertEquals("http://www.google.com/", request.getRequestURI().toString());

    config.setAbsoluteUriIsAllowed(false);
    try {
      request.setRequestLineMembers("GET http://www.google.com/ HTTP/1.1");
      fail();
    } catch (HttpError e) {
      assertEquals(BAD_REQUEST, e.getErrorCode());
    }
  }

  @Test
  public void testSetRequestURIRelativeURIContainsPort() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("host", "www.google.com:8888");
    request.setRequestLineMembers("GET / HTTP/1.1");

    assertEquals("www.google.com", request.getRequestURI().getHost());
    assertEquals(8888, request.getRequestURI().getPort());
    assertEquals("/", request.getRequestURI().getPath());
    assertEquals("http://www.google.com:8888/", request.getRequestURI().toString());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestURIAbsoluteURIContainsPort() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setRequestLineMembers("GET http://www.google.com:8888/ HTTP/1.1");

    assertEquals("www.google.com", request.getRequestURI().getHost());
    assertEquals(8888, request.getRequestURI().getPort());
    assertEquals("/", request.getRequestURI().getPath());
    assertEquals("http://www.google.com:8888/", request.getRequestURI().toString());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestURIContainsFragment() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("host", "www.google.com");
    request.setRequestLineMembers("GET /?a=b#abc HTTP/1.1");

    assertEquals("/", request.getRequestURI().getPath());
    assertEquals("a=b", request.getRequestURI().getQuery());
    assertEquals("abc", request.getRequestURI().getFragment());
    assertEquals("http://www.google.com/?a=b#abc", request.getRequestURI().toString());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestSingleParameterIsCorrectlyParsed() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("host", "www.google.com");
    request.setRequestLineMembers("GET /test?a HTTP/1.1");

    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals(null, request.getParameters().get("a").get(0));

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestQueryIsCorrectlyParsed() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("host", "www.google.com");
    request.setRequestLineMembers("GET /test?a&b=1&&c=&d=5&d&a=8&d=abc%20d HTTP/1.1");

    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals(null, request.getParameters().get("a").get(0));
    assertEquals("1", request.getParameters().get("b").get(0));
    assertEquals("", request.getParameters().get("c").get(0));
    assertEquals("5", request.getParameters().get("d").get(0));
    assertEquals(null, request.getParameters().get("d").get(1));
    assertEquals("8", request.getParameters().get("a").get(1));
    assertEquals("abc d", request.getParameters().get("d").get(2));

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorPostFormUrlEncodedDataParsedCorrectly_HTML401_specification_17_13_4() throws Exception {
    String requestString = "GET /test HTTP/1.1\r\n" +
      "Host: www.google.com\r\n" +
      "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" +
      "Content-Length: 31\r\n\r\n" +
      "a&b=1&&c=&d=5&d&a=8+9&d=abc%20d";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());
    Request request = new Request(in, serverConfiguration);

    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals(null, request.getParameters().get("a").get(0));
    assertEquals("1", request.getParameters().get("b").get(0));
    assertEquals("", request.getParameters().get("c").get(0));
    assertEquals("5", request.getParameters().get("d").get(0));
    assertEquals(null, request.getParameters().get("d").get(1));
    assertEquals("8 9", request.getParameters().get("a").get(1));
    assertEquals("abc d", request.getParameters().get("d").get(2));
    assertEquals("a&b=1&&c=&d=5&d&a=8+9&d=abc%20d", request.getBody());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorPostNonFormUrlEncodedDataNotParsed_HTML401_specification_17_13_4() throws Exception {
    String requestString = "GET /test HTTP/1.1\r\n" +
      "Host: www.google.com\r\n" +
      "Content-Length: 31\r\n\r\n" +
      "a&b=1&&c=&d=5&d&a=8+9&d=abc%20d";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());
    Request request = new Request(in, serverConfiguration);

    assertEquals(null, request.getParameters());
    assertEquals("a&b=1&&c=&d=5&d&a=8+9&d=abc%20d", request.getBody());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorMultipartContentTypeNotSupported_HTML401_specification_17_13_4() throws Exception {
    String requestString = "GET /test HTTP/1.1\r\n" +
      "Host: www.google.com\r\n" +
      "Content-Length: 31\r\n" +
      "Content-Type: multipart/form-data\r\n\r\n" +
      "a&b=1&&c=&d=5&d&a=8+9&d=abc%20d";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());
    Request request = new Request(in, serverConfiguration);

    assertEquals(NOT_IMPLEMENTED, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorContentHeaderWithoutBodyIsBadRequest() throws Exception {
    String requestString = "GET /test HTTP/1.1\r\n" +
      "Host: www.google.com\r\n" +
      "Content-whatever: foo bar\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());
    Request request = new Request(in, serverConfiguration);

    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
  }

  @Test
  public void testSetRequestQueryContainsEncodedDelimiters() throws Exception {
    Request request = new Request(serverConfiguration);
    request.setHeader("host", "www.google.com");
    request.setRequestLineMembers("GET /test?a=A%26B&b=c%26d%3De HTTP/1.1");

    assertEquals("/test", request.getRequestURI().getPath());
    assertEquals("a=A&B&b=c&d=e", request.getRequestURI().getQuery());
    assertEquals("a=A%26B&b=c%26d%3De", request.getRequestURI().toURL().getQuery());
    assertEquals("A&B", request.getParameters().get("a").get(0));
    assertEquals("c&d=e", request.getParameters().get("b").get(0));
    assertEquals("http://www.google.com/test?a=A%26B&b=c%26d%3De", request.getRequestURI().toString());

    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorReadNonASCIIHeadersInISO_8859_1Charset() throws Exception {
    String requestString = "GET /test.html HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Non-ASCII-header: a\u00C8b\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes(StandardCharsets.ISO_8859_1));

    Request request = new Request(in, serverConfiguration);
    assertEquals("a\u00C8b", request.getHeader("Non-ASCII-header"));
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorReadNonASCIIHeadersInOtherThanISO_8859_1Charset() throws Exception {
    String requestString = "GET /test.html HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Non-ASCII-header: a\u00C8b\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());

    Request request = new Request(in, serverConfiguration);
    assertNotEquals("a\u00C8b", request.getHeader("Non-ASCII-header"));
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorReadBodyInOtherThanDefaultCharset() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-Type: text/html; charset=UTF-16\r\n" +
      "Content-length: 12\r\n\r\n";
    InputStream in = new ByteArrayInputStream(mergeByteArrays(
      requestString.getBytes(StandardCharsets.ISO_8859_1),
      "body\u00C8".getBytes(StandardCharsets.UTF_16)
    ));

    Request request = new Request(in, serverConfiguration);
    assertEquals("body\u00C8", request.getBody());
    assertEquals(null, request.getResponseStatusCode());
  }

  @Test
  public void testConstructorReadBodyInDefaultCharset() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-length: 5\r\n\r\n";
    InputStream in = new ByteArrayInputStream(mergeByteArrays(
      requestString.getBytes(StandardCharsets.ISO_8859_1),
      "body\u00C8".getBytes(StandardCharsets.ISO_8859_1)
    ));

    Request request = new Request(in, serverConfiguration);
    assertEquals("body\u00C8", request.getBody());
    assertEquals(null, request.getResponseStatusCode());
  }

  private byte[] mergeByteArrays(byte[] firstArray, byte[] secondArray) {
    byte[] resultingArray = new byte[firstArray.length + secondArray.length];
    System.arraycopy(firstArray, 0, resultingArray, 0, firstArray.length);
    System.arraycopy(secondArray, 0, resultingArray, firstArray.length, secondArray.length);
    return resultingArray;
  }
}