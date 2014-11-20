package core;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RequestProcessorTest {

  Socket clientSocket;
  RequestProcessor processor;
  Map<Pattern, Handler> handlers;
  Configuration configuration;

  @Before
  public void setUpStreams() {
    clientSocket = mock(Socket.class);
    handlers = new LinkedHashMap<>();
    configuration = new Configuration();
    processor = spy(new RequestProcessor(
      clientSocket,
      configuration,
      handlers
      ));
  }

  @Test
  public void testProcessError() throws Exception {
    PrintStream savedOut = System.out;
    OutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out));

    when(clientSocket.getOutputStream()).thenReturn(mock(OutputStream.class));
    when(clientSocket.getInputStream()).thenReturn(mock(InputStream.class));
    doThrow(new RuntimeException("test")).
      when(processor).process(any(OutputStream.class), any(InputStream.class));

    processor.run();
    assertEquals("Exception caught:\ntest\n", out.toString());

    System.setOut(savedOut);
  }

  @Test
  public void testHandlerError() throws Exception {
    OutputStream out = new ByteArrayOutputStream();
    InputStream in = new ByteArrayInputStream("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(ISO_8859_1));

    doThrow(new RuntimeException("test")).
      when(processor).executeHandlers(any(Request.class), any(Response.class));

    processor.process(out, in);
    assertEquals("HTTP/1.1 500 Internal Server Error", out.toString().split("\r\n")[0]);
  }

  @Test
  public void testHandleNoHandlers() throws Exception {
    handlers.clear();

    OutputStream out = new ByteArrayOutputStream();
    InputStream in = new ByteArrayInputStream("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(ISO_8859_1));

    processor.process(out, in);
    assertEquals("HTTP/1.1 404 Not Found", out.toString().split("\r\n")[0]);
  }

  @Test
  public void testHandleMultipleHandlers() throws Exception {
    handlers.clear();
    handlers.put(Pattern.compile(".*"), new HandlerOK());
    handlers.put(Pattern.compile("/abc/.*"), new HandlerNotFound());

    OutputStream out = new ByteArrayOutputStream();
    InputStream in = new ByteArrayInputStream("GET /test.html HTTP/1.1\r\nHost: www.google.com\r\n\r\n".getBytes(ISO_8859_1));
    processor.process(out, in);
    assertEquals("HTTP/1.1 " + OK, out.toString().split("\r\n")[0]);
    assertEquals("foo", out.toString().split("\r\n\r\n")[1]);

    out = new ByteArrayOutputStream();
    in = new ByteArrayInputStream("GET /abc/test.html HTTP/1.1\r\nHost: www.google.com\r\n\r\n".getBytes(ISO_8859_1));
    processor.process(out, in);
    assertEquals("HTTP/1.1 " + NOT_FOUND, out.toString().split("\r\n")[0]);
    assertEquals("foobar", out.toString().split("\r\n\r\n")[1]);
  }

  public static class HandlerOK extends Handler {
    @Override
    public void handle(Request request, Response response) {
      response.responseStatusCode = OK;
      response.setBody("foo");
    }
  }

  public static class HandlerNotFound extends Handler {
    @Override
    public void handle(Request request, Response response) {
      response.responseStatusCode = NOT_FOUND;
      response.setBody(response.getBody() + "bar");
    }
  }
}