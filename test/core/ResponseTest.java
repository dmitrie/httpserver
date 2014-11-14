package core;

import org.junit.Before;
import org.junit.Test;
import util.LinkedCaseInsensitiveMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;

public class ResponseTest {

  ServerConfiguration serverConfiguration = new ServerConfiguration();

  @Before
  public void setUp() throws Exception {
    serverConfiguration.setAbsoluteUriIsAllowed(true);
  }

  @Test
  public void testConstructorUsesErrorFromRequest() throws Exception {
    String requestString = "foo bar\r\n\r\n";
    InputStream in = new ByteArrayInputStream(requestString.getBytes());
    Request request = new Request(in, serverConfiguration);

    Response response = new Response(request);
    assertEquals(BAD_REQUEST, request.getResponseStatusCode());
    assertEquals(BAD_REQUEST, response.getResponseStatusCode());
  }

  @Test
  public void testSetErrorBodyAndHeadersDefaultResponse() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setErrorBodyAndHeaders(HTTP_VERSION_NOT_SUPPORTED);

    assertEquals(HTTP_VERSION_NOT_SUPPORTED, response.getResponseStatusCode());
    assertEquals(HTTP_VERSION_NOT_SUPPORTED.toString(), response.getBody());
  }

  @Test
  public void testGetContentLengthUsesCurrentCharset() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setBody("test\n");
    response.setBodyCharset(StandardCharsets.UTF_8);
    assertEquals("5", response.getContentLength());
    response.setBodyCharset(StandardCharsets.UTF_16);
    assertEquals("12", response.getContentLength());
  }

  @Test
  public void testGenerateMessage_RFC2616_6() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setBodyCharset(StandardCharsets.UTF_8);
    response.setBody("test\r\ntest\t");
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setResponseStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: 11\r\n\r\ntest\r\ntest\t", response.generateMessage());
  }

  @Test
  public void testGenerateMessageNoHeadersAndNoBody_RFC2616_6() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setResponseStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\n\r\n", response.generateMessage());
  }

  @Test
  public void testGenerateMessageNoBody_RFC2616_6() throws Exception {
    Response response = new Response(new Request(new ByteArrayInputStream("HEAD http://google.com HTTP/1.1".getBytes()), serverConfiguration));
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setResponseStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n", response.generateMessage());
  }

  @Test
  public void testGenerateMessageNoHeaders_RFC2616_6() throws Exception {
    Response response = new Response(new Request(new ByteArrayInputStream("POST http://google.com HTTP/1.1".getBytes()), serverConfiguration));
    response.setBodyCharset(StandardCharsets.UTF_8);
    response.setBody("test\r\ntest\t");
    response.setResponseStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Length: 11\r\n\r\ntest\r\ntest\t", response.generateMessage());
  }

  @Test
  public void testHeaderNamesAreCaseInsensitive_RFC2616_4_2() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setHeaders(new LinkedCaseInsensitiveMap(){{ put("Content-Type", "text/html"); }});

    assertEquals("text/html", response.getHeader("CONTENT-Type"));
    response.setHeader("Content-TYPE", "text/css");
    assertEquals("text/css", response.getHeader("Content-Type"));
  }

  @Test
  public void testHeaderNamesCaseIsPreserved() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setHeader("Content-Type", "text/html");
    assertEquals("Content-Type", response.getHeaders().keySet().iterator().next());
  }

  @Test
  public void testSetHeaderOverwritesContentLength() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.setBodyCharset(StandardCharsets.UTF_8);
    response.setBody("test\r\ntest\t");
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.setHeader("Content-Length", "1");
    response.setResponseStatusCode(OK);
    assertEquals("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: 11\r\n\r\ntest\r\ntest\t", response.generateMessage());
  }

  @Test
  public void testGenerateMessageRemovesBodyFromResponseToHead_RFC2616_14_13() throws Exception {
    Response response = new Response(new Request(serverConfiguration));
    response.getRequest().setMethod("HEAD");
    response.setResponseStatusCode(OK);
    response.setBody("test");
    response.setHeader("Content-Length", "4");
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    assertEquals("HTTP/1.1 200 OK\r\nContent-Length: 4\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n", response.generateMessage());
  }
}
