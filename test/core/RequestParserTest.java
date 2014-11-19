package core;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import static core.HttpStatusCode.*;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.*;

public class RequestParserTest {
  RequestParser requestParser;
  Configuration configuration;

  @Before
  public void setUp() throws Exception {
    configuration = new Configuration();
    requestParser = new RequestParser(configuration);
  }

  private ByteArrayInputStream in(String httpPacket) {
    return new ByteArrayInputStream(httpPacket.getBytes(ISO_8859_1));
  }

  private void assertHttpError(String httpPacket, HttpStatusCode error) {
    try {
      requestParser.parse(in(httpPacket));
      fail();
    } catch (HttpError e) {
      assertEquals(error, e.getErrorCode());
    }
  }

  private byte[] mergeByteArrays(byte[] firstArray, byte[] secondArray) {
    byte[] resultingArray = new byte[firstArray.length + secondArray.length];
    System.arraycopy(firstArray, 0, resultingArray, 0, firstArray.length);
    System.arraycopy(secondArray, 0, resultingArray, firstArray.length, secondArray.length);
    return resultingArray;
  }

  private Request parse(String httpPacket) {
    requestParser.parse(in(httpPacket));
    return requestParser.request;
  }

  @Test
  public void testReadStartLine_RFC2616_4_1() throws Exception {
    assertEquals("request line", requestParser.readStartLineAndHeaders(in("request line\r\n\r\nbody")));
  }

  @Test
  public void testReadStartLineAndHeaders_RFC2616_4_1() throws Exception {
    assertEquals("request line\r\nheader", requestParser.readStartLineAndHeaders(in("request line\r\nheader\r\n\r\nbody")));
  }

  @Test
  public void testReadStartLineAndBody_RFC2616_4_1() throws Exception {
    InputStream in = in("request line\r\n\r\nbody");
    assertEquals("request line", requestParser.readStartLineAndHeaders(in));
    assertEquals("body", requestParser.readExactNumberOfBytes(in, 4));
  }

  @Test
  public void testReadStartLineAndHeadersAndBody_RFC2616_4_1() throws Exception {
    InputStream in = in("request line\r\nheader 1\r\nheader 2\r\n\r\nbody\r\n\r\nbody");
    assertEquals("request line\r\nheader 1\r\nheader 2", requestParser.readStartLineAndHeaders(in));
    assertEquals("body\r\n\r\nbody", requestParser.readExactNumberOfBytes(in, 12));
  }

  @Test
  public void testSkipNewLinesBeforeStartLine_RFC2616_4_1() throws Exception {
    assertEquals("abc\r\ndef", requestParser.readStartLineAndHeaders(in("\r\n\r\nabc\r\ndef\r\n\r\n")));
  }

  @Test
  public void testReadNonAsciiHeaders() throws Exception {
    assertEquals("abc\u00FF", requestParser.readStartLineAndHeaders(in("abc\u00FF\r\n\r\n")));
  }

  @Test
  public void testMethod() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("POST", request.method);
  }

  @Test
  public void testRequestURI() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("/", request.requestURI.getPath());
  }

  @Test
  public void testHttpVersion() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("HTTP/1.1", request.httpVersion);
  }

  @Test
  public void testInvalidMethod_RFC2616_5_1_1() throws Exception {
    assertHttpError("~!#$@!%#^&$ / HTTP/1.1\r\nHost: localhost\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testInvalidRequestURI() throws Exception {
    assertHttpError("GET /\\\\ HTTP/1.1\r\nHost: localhost\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testRelativeRequestURI() throws Exception {
    assertHttpError("GET test.html HTTP/1.1\r\nHost: localhost\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testInvalidHost() throws Exception {
    assertHttpError("GET / HTTP/1.1\r\nHost: @#^%$&\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testHostWithPort() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost:8888\r\n\r\n");
    assertEquals(8888, request.requestURI.getPort());
  }

  @Test
  public void testRelativeRequestUri() throws Exception {
    assertHttpError("GET test.html HTTP/1.1\r\nHost: localhost\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testInvalidHttpVersion_RFC2616_3_1() throws Exception {
    assertHttpError("GET / HTTP99\r\nHost: localhost\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testInvalidStartLineFormat_RFC2616_5_1() throws Exception {
    assertHttpError("GET / HTTP/1.1 FOO BAR\r\nHost: localhost\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testHeaders() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\nUser-Agent: java\r\n\r\n");
    assertEquals("localhost", request.getHeader("Host"));
    assertEquals("java", request.getHeader("User-Agent"));
  }

  @Test
  public void testInvalidHeaderLine_RFC2616_4_2() throws Exception {
    assertHttpError("GET / HTTP/1.1\r\nHost: localhost\r\nHeader test\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testNewLinesInHeaderValues_RFC2616_4_2() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\nUser-Agent: ja\r\n\t   \r\n va\r\n\r\n");
    assertEquals("ja va", request.getHeader("User-Agent"));
  }

  @Test
  public void testNoSpacesAllowedInHeaderName_RFC2616_4_2() throws Exception {
    assertHttpError("GET / HTTP/1.1\r\nHost: localhost\r\nHea der: test\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testHeaderValueIsTrimmed_RFC2616_4_2() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost:   localhost \r\n  \r\n\r\n");
    assertEquals("localhost", request.getHeader("Host"));
  }

  @Test
  public void testMergeMultipleHeadersWithSameName_RFC2616_4_2() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nUser-Agent: browser\r\nHost: localhost\r\nUser-Agent: java\r\n\r\n");
    assertEquals("browser, java", request.getHeader("User-Agent"));
  }

  @Test
  public void testHeaderNamesAreCaseInsensitive_RFC2616_4_2() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("localhost", request.getHeader("HOST"));
    assertEquals("localhost", request.getHeader("host"));
  }

  @Test
  public void testHeaderNameCaseIsPreserved() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertTrue(request.headers.keySet().contains("Host"));
    assertFalse(request.headers.keySet().contains("HOST"));
  }

  @Test
  public void testAbsoluteURINotImplemented_RFC2616_5_1_2() throws Exception {
    assertHttpError("GET http://www.google.com/ HTTP/1.1\r\nHost: localhost\r\n\r\n", NOT_IMPLEMENTED);
  }

  @Test
  public void testHostHeaderMustBePresent_RFC2616_5_1_2() throws Exception {
    assertHttpError("GET / HTTP/1.1\r\n\r\n", BAD_REQUEST);
    assertHttpError("GET / HTTP/1.1\r\nUser-Agent: java\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testAbsPathContainsFragment() throws Exception {
    Request request = parse("GET /test.html#def HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("def", request.requestURI.getFragment());
  }

  @Test
  public void testMergeHostAndAbsPathIntoRequestURI() throws Exception {
    Request request = parse("GET /test.html?abc=123&a#def HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("http://localhost/test.html?abc=123&a#def", request.requestURI.toString());
  }

  @Test
  public void testTransferEncodingHeaderNotImplemented() throws Exception {
    assertHttpError("GET http://www.google.com/ HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n", NOT_IMPLEMENTED);
  }

  @Test
  public void testAsteriskRequestUriNotImplemented_RFC2616_5_1_2() throws Exception {
    assertHttpError("POST * HTTP/1.1\r\nHost: localhost\r\n\r\n", NOT_IMPLEMENTED);
  }

  @Test
  public void testTooLongRequestUri_RFC2616_10_4_15() throws Exception {
    int defaultMaximumURILength = configuration.getMaximumURILength();
    configuration.setMaximumURILength(5);
    assertHttpError("GET /1234567 HTTP/1.1\r\nHost: localhost\r\n\r\n", REQUEST_URI_TOO_LONG);
    configuration.setMaximumURILength(defaultMaximumURILength);
  }

  @Test
  public void testUnsupportedHttpVersion_RFC2616_10_5_6() throws Exception {
    assertHttpError("GET / HTTP/0.9\r\nHost: localhost\r\n\r\n", HTTP_VERSION_NOT_SUPPORTED);
  }

  @Test
  public void testUnsupportedMethod_RFC2616_5_1_1() throws Exception {
    assertHttpError("ANYMETHOD / HTTP/1.1\r\nHost: localhost\r\n\r\n", NOT_IMPLEMENTED);
  }

  @Test
  public void testParseQuery() throws Exception {
    Request request = parse("GET /?b=test&a=abc&a&a=&b=%26123#anchor HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals(new LinkedList<String>() {{
      add("abc");
      add(null);
      add("");
    }}, request.parameters.get("a"));
    assertEquals(new LinkedList<String>() {{
      add("test");
      add("&123");
    }}, request.parameters.get("b"));
  }

  @Test
  public void testParseEmptyQuery() throws Exception {
    Request request = parse("GET /? HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertTrue(request.parameters.isEmpty());
  }

  @Test
  public void testQueryIsParsedForGetRequestsOnly() throws Exception {
    Request request = parse("POST /?b=test&a=abc&a&a=&b=%26123#anchor HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertTrue(request.parameters.isEmpty());
  }

  @Test
  public void testSkipBodyIfThereIsNoContentLengthHeader_RFC2616_4_4() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\n\r\nbody here");
    assertEquals(null, request.body);
  }

  @Test
  public void testSkipBodyIfContentLengthIsZero_RFC2616_4_4() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\n\r\nbody here");
    assertEquals("", request.body);
  }

  @Test
  public void testContentLengthIsNotInteger_RFC2616_4_4() throws Exception {
    assertHttpError("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: abc\r\n\r\nbody here", BAD_REQUEST);
  }

  @Test
  public void testBodyLengthIsSmallerThanContentLength_RFC2616_4_4() throws Exception {
    assertHttpError("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 99\r\n\r\nbody here", BAD_REQUEST);
  }

  @Test
  public void testBodyIsCutToContentLength_RFC2616_4_4() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 4\r\n\r\nlong long body");
    assertEquals("long", request.body);
  }

  @Test
  public void testBody() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 9\r\n\r\nbody here");
    assertEquals("body here", request.body);
  }

  @Test
  public void testBodyIsNotTrimmedAndNoLWSAreReplaced() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 19\r\n\r\n\r\nbody \r\n \there\r\n\t ");
    assertEquals("\r\nbody \r\n \there\r\n\t ", request.body);
  }

  @Test
  public void testBodyInOtherCharset() throws Exception {
    String requestString = "POST /test HTTP/1.0\r\n" +
      "Host: www.google.com\r\n" +
      "Content-Type: text/html; charset=UTF-16\r\n" +
      "Content-length: 12\r\n\r\n";
    InputStream in = new ByteArrayInputStream(mergeByteArrays(
      requestString.getBytes(StandardCharsets.ISO_8859_1),
      "body\uFFFF".getBytes(StandardCharsets.UTF_16)
    ));

    requestParser.parse(in);
    Request request = requestParser.request;
    assertEquals("body\uFFFF", request.body);
  }

  @Test
  public void testUrlencodedBodyIsParsed_HTML401_specification_17_13_4() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 20\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\na=1&a=&bc=12+%26%203");
    assertEquals("a=1&a=&bc=12+%26%203", request.body);

    assertEquals(new LinkedList<String>() {{
      add("1");
      add("");
    }}, request.parameters.get("a"));
    assertEquals(new LinkedList<String>() {{
      add("12 & 3");
    }}, request.parameters.get("bc"));
  }


  @Test
  public void testParseEmptyBody() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\n");
    assertTrue(request.parameters.isEmpty());
  }

  @Test
  public void testMultipartBodyNotImplemented_HTML401_specification_17_13_4() throws Exception {
    assertHttpError("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 4\r\nContent-Type: multipart/form-data\r\n\r\nbody", NOT_IMPLEMENTED);
  }

  @Test
  public void testBodyWithOtherContentTypeIsNotParsed() throws Exception {
    Request request = parse("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 20\r\nContent-Type: test\r\n\r\na=1&a=&bc=12+%26%203");
    assertEquals("a=1&a=&bc=12+%26%203", request.body);
    assertTrue(request.parameters.isEmpty());
  }

  @Test
  public void testQueryIsParsedForPostRequestsOnly() throws Exception {
    Request request = parse("GET / HTTP/1.1\r\nHost: localhost\r\nContent-Length: 20\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\na=1&a=&bc=12+%26%203");
    assertTrue(request.parameters.isEmpty());
  }

  @Test
  public void testContentHeadersRequireBody() throws Exception {
    assertHttpError("GET / HTTP/1.1\r\nHost: localhost\r\nContent-Type: test\r\n\r\n", BAD_REQUEST);
  }

  @Test
  public void testSetFieldsSuccess() throws Exception {
    Request request = requestParser.setFields(in("GET /?a=1#abc HTTP/1.1\r\nHost: localhost\r\nContent-Length: 4\r\n\r\nbody"));
    assertEquals("GET", request.method);
    assertEquals("http://localhost/?a=1#abc", request.requestURI.toString());
    assertEquals("HTTP/1.1", request.httpVersion);
    assertEquals("body", request.body);
    assertEquals("1", request.parameters.get("a").get(0));
    assertEquals(null, request.responseStatusCode);
  }

  @Test
  public void testSetFieldsFailure() throws Exception {
    Request request = requestParser.setFields(in("GET /?a=1#abc HTTP/0.9\r\nHost: localhost\r\nContent-Length: 4\r\n\r\nbody"));
    assertEquals("GET", request.method);
    assertEquals("/?a=1#abc", request.requestURI.toString());
    assertEquals("HTTP/0.9", request.httpVersion);
    assertEquals(null, request.body);
    assertEquals(null, request.parameters.get("a"));
    assertEquals(HTTP_VERSION_NOT_SUPPORTED, request.responseStatusCode);
  }

  @Test
  public void testUseLFInsteadOfCRLF_RFC2616_2_2() throws Exception {
    assertHttpError("GET / HTTP/1.1\nHost: localhost\n\n", BAD_REQUEST);
  }
}

