package core;

import org.junit.Test;

import static core.HttpStatusCode.*;
import static org.junit.Assert.*;

public class ResponseTest {

  @Test
  public void testSetDefaultHttpVersionInCaseOfEmptyConstructor() throws Exception {
    Response response = new Response();
    assertEquals("HTTP/1.1", response.httpVersion);
  }

  @Test
  public void testUseResponseStatusCodeFromRequest() throws Exception {
    Request request = new Request();
    request.responseStatusCode = NOT_FOUND;

    Response response = new Response(request);
    assertEquals(NOT_FOUND, response.responseStatusCode);
  }

  @Test
  public void testUseHttpVersionFromRequest() throws Exception {
    Request request = new Request();
    request.httpVersion = "HTTP/1.0";

    Response response = new Response(request);
    assertEquals("HTTP/1.0", response.httpVersion);
  }

  @Test
  public void testUseRequestMethodFromRequest() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";

    Response response = new Response(request);
    assertEquals("GET", response.requestMethod);
  }

  @Test
  public void testGenerateErrorResponse() throws Exception {
    Request request = new Request();
    request.responseStatusCode = NOT_FOUND;

    Response response = new Response(request);
    assertEquals(NOT_FOUND, response.responseStatusCode);
    assertEquals(NOT_FOUND.toString(), response.body);
  }

  @Test
  public void testSetStandardHeadersInErrorResponse() throws Exception {
    Request request = new Request();
    request.responseStatusCode = NOT_IMPLEMENTED;

    Response response = new Response(request);
    assertTrue(response.getHeader("Content-Type") != null);
    assertTrue(response.getHeader("Last-modified") != null);
  }

  @Test
  public void testSetBody() throws Exception {
    Response response = new Response();
    response.setBody("test body");

    assertEquals("test body", response.getBody());
  }

  @Test
  public void testSetContentLengthWhenBodyIsSet() throws Exception {
    Response response = new Response();
    response.setBody("test body");

    assertEquals("9", response.getHeader("Content-Length"));
  }

  @Test
  public void testNoBodyIsAllowedForResponsesToHeadRequests_RFC2616_9_4() throws Exception {
    Response response = new Response();
    response.requestMethod = "HEAD";
    response.setBody("Some body here");

    assertEquals(null, response.body);
    assertEquals("14", response.getHeader("Content-Length"));
  }

  @Test
  public void testMakeSureNoContentHeadersAreSentWithoutBody() throws Exception {
    Response response = new Response();

    response.setHeader("Content-Type", "test");
    assertTrue(!response.contentHeadersAreCorrect());

    response.setBody("test");
    assertTrue(response.contentHeadersAreCorrect());
  }

  @Test
  public void testAllowContentHeadersForResponsesToHeadRequests_RFC2616_9_4() throws Exception {
    Response response = new Response();
    response.requestMethod = "HEAD";

    response.setHeader("Content-Type", "test");
    assertTrue(response.contentHeadersAreCorrect());
  }

  @Test
  public void testGenerateMessageWithoutHeaders_RFC2616_6() throws Exception {
    Response response = new Response();
    response.responseStatusCode = OK;

    assertEquals("HTTP/1.1 200 OK\r\n\r\n", response.generateMessage());
  }

  @Test
  public void testGenerateMessageWithHeaders_RFC2616_6() throws Exception {
    Response response = new Response();
    response.responseStatusCode = OK;
    response.setHeader("Header1", "value 1");
    response.setHeader("Header2", "value 2");

    assertEquals("HTTP/1.1 200 OK\r\nHeader1: value 1\r\nHeader2: value 2\r\n\r\n", response.generateMessage());
  }

  @Test
  public void testGenerateMessageWithHeadersAndBody_RFC2616_6() throws Exception {
    Response response = new Response();
    response.responseStatusCode = OK;
    response.setHeader("Header1", "value 1");
    response.setBody("test");

    assertEquals("HTTP/1.1 200 OK\r\nHeader1: value 1\r\nContent-Length: 4\r\n\r\ntest", response.generateMessage());
  }

  @Test
  public void testGenerateMessageWithBodyAndNoHeaders_RFC2616_6() throws Exception {
    Response response = new Response();
    response.responseStatusCode = OK;
    response.body = "test";

    assertEquals("HTTP/1.1 200 OK\r\n\r\ntest", response.generateMessage());
  }

  @Test
  public void testGenerateMessageValidatesStatusCodePresence() throws Exception {
    Response response = new Response();
    response.httpVersion = "HTTP/1.1";

    try {
      response.generateMessage();
      fail();
    } catch (RuntimeException e) {
      assertEquals("Cannot generate a valid HTTP response without status code", e.getMessage());
    }
  }

  @Test
  public void testGenerateMessageValidatesHttpVersionPresence() throws Exception {
    Response response = new Response();
    response.responseStatusCode = OK;
    response.httpVersion = null;

    try {
      response.generateMessage();
      fail();
    } catch (RuntimeException e) {
      assertEquals("Cannot generate a valid HTTP response without HTTP version", e.getMessage());
    }
  }

  @Test
  public void testGenerateMessageValidatesContentHeaders() throws Exception {
    Response response = new Response();
    response.responseStatusCode = OK;
    response.setHeader("Content-something", "test");

    try {
      response.generateMessage();
      fail();
    } catch (RuntimeException e) {
      assertEquals("Content-* headers are not allowed without body in response to non-HEAD requests", e.getMessage());
    }
  }
}
