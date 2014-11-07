package core;

import org.junit.Test;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;

public class ResponseTest {

  @Test
  public void testConstructorUsesErrorFromRequest() throws Exception {
    String requestString = "foo bar\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());
    Request request = new Request(in);

    Response response = new Response(request);
    assertEquals(BAD_REQUEST, request.getErrorCode());
    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void testSetErrorBodyAndHeadersDefaultResponse() throws Exception {
    Response response = new Response(new Request());
    response.setErrorBodyAndHeaders(HTTP_VERSION_NOT_SUPPORTED);

    assertEquals(HTTP_VERSION_NOT_SUPPORTED, response.getStatusCode());
    assertEquals(HTTP_VERSION_NOT_SUPPORTED.toString(), response.getBody());
  }

  @Test
  public void testGetContentLengthUsesCurrentEncoding() throws Exception {
    Response response = new Response(new Request());
    response.setBody("test\n");
    response.setEncoding("UTF-8");
    assertEquals(5, response.getContentLength());
    response.setEncoding("UTF-16");
    assertEquals(12, response.getContentLength());
  }

  @Test
  public void testToString_RFC2616_6() throws Exception {
    Response response = new Response(new Request());
    response.setEncoding("UTF-8");
    response.setBody("test\r\ntest\t");
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: 11\r\n\r\ntest\r\ntest\t", response.toString());
  }

  @Test
  public void testToStringNoHeadersAndNoBody_RFC2616_6() throws Exception {
    Response response = new Response(new Request());
    response.setStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\n\r\n", response.toString());
  }

  @Test
  public void testToStringNoBody_RFC2616_6() throws Exception {
    Response response = new Response(new Request(new ByteArrayInputStream("HEAD http://google.com HTTP/1.1".getBytes())));
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n", response.toString());
  }

  @Test
  public void testToStringNoHeaders_RFC2616_6() throws Exception {
    Response response = new Response(new Request(new ByteArrayInputStream("HEAD http://google.com HTTP/1.1".getBytes())));
    response.setEncoding("UTF-8");
    response.setBody("test\r\ntest\t");
    response.setStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Length: 11\r\n\r\ntest\r\ntest\t", response.toString());
  }

  @Test
  public void testHeaderNamesAreCaseInsensitive_RFC2616_4_2() throws Exception {
    Response response = new Response(new Request());

    response.setHeaders(new LinkedCaseInsensitiveMap<String>(){{
      put("Content-Type", "text/html");
    }});

    assertEquals("text/html", response.getHeader("CONTENT-Type"));
    response.setHeader("Content-TYPE", "text/css");
    assertEquals("text/css", response.getHeader("Content-Type"));
  }

  @Test
  public void testHeaderNamesCaseIsPreserved() throws Exception {
    Response response = new Response(new Request());
    response.setHeader("Content-Type", "text/html");
    assertEquals("Content-Type", response.getHeaders().keySet().iterator().next());
  }

  @Test (expected = IllegalArgumentException.class)
  public void testSetHeaderNoContentLengthAllowed() throws Exception {
    Response response = new Response(new Request());
    response.setHeader("CONTENT-length", "50");
  }
}
