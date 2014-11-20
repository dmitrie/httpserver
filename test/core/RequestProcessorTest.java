package core;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RequestProcessorTest {

  Socket clientSocket;
  RequestProcessor processor;

  @Before
  public void setUpStreams() {
    clientSocket = mock(Socket.class);
    processor = spy(new RequestProcessor(
      clientSocket,
      mock(Configuration.class),
      mock(Map.class)));
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
}