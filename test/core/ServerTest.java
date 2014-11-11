package core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

import static core.HttpRequestRegEx.CRLF;
import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;

public class ServerTest {
  Thread serverThread;
  Server server;

  @Before
  public void setUp() throws Exception {
    int numberOfAttempts = 5;
    do {
      try {
        server = new Server(getConfiguration());
        serverThread = new Thread(server::start);
        serverThread.start();
        return;
      } catch (Exception e) {
        Thread.sleep(500);
      }
    } while (numberOfAttempts-- > 0);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
    serverThread.interrupt();
  }

  private ServerConfiguration getConfiguration() {
    ServerConfiguration serverConfiguration = new ServerConfiguration();
    serverConfiguration.setPortNumber(8361);
    serverConfiguration.setDocumentRootPath("/home/kool/IdeaProjects/httpserver/test/web/");
    serverConfiguration.setRequestTimeOut(500);
    return serverConfiguration;
  }

  public IncomingHttpMessage sendRequest(String request) throws IOException {
    IncomingHttpMessage serverResponse;
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream()
    ) {
      out.write(request);
      out.flush();

      serverResponse = readServerResponse(in);
    }
    return serverResponse;
  }

  private IncomingHttpMessage readServerResponse(InputStream in) throws IOException {
    IncomingHttpMessage serverResponse = new IncomingHttpMessage();
    String[] statusLineAndHeaders = serverResponse.readStartLineAndHeaders(in).split(CRLF, 2);

    serverResponse.setStartLine(statusLineAndHeaders[0]);
    serverResponse.parseAndSetHeaders(statusLineAndHeaders[1]);

    serverResponse.setBody(serverResponse.readBody(in));

    return serverResponse;
  }

  @Test
  public void testHtmlFileHandlerSuccessFileReturned() throws Exception {
    IncomingHttpMessage response = sendRequest(
      "GET /test.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

    assertEquals("HTTP/1.1 " + OK, response.getStartLine());
    assertEquals("16", response.getHeader("Content-length"));
    assertEquals("<h1>Example</h1>", response.getBody());
  }

  @Test
  public void testHtmlFileHandlerSuccessFileWithSpaceInNameReturned() throws Exception {
    IncomingHttpMessage response = sendRequest(
      "GET /folder/test%20file%201.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

    assertEquals("HTTP/1.1 " + OK, response.getStartLine());
    assertEquals("28", response.getHeader("Content-length"));
    assertEquals("<h2>Example 1 in folder</h2>", response.getBody());
  }

  @Test
  public void testHtmlFileHandlerFileNotFound() throws Exception {
    assertEquals("HTTP/1.1 " + NOT_FOUND, sendRequest("GET /foo/bar/test.html HTTP/1.1\r\nHost: localhost\r\n\r\n").getStartLine());
  }

  @Test
  public void testHtmlFileHandlerBadRequest() throws Exception {
    assertEquals("HTTP/1.1 " + BAD_REQUEST, sendRequest("foo bar\r\n\r\n").getStartLine());
  }

  @Test
  public void testRespondWithErrorRequestTimeOut() throws Exception {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      InputStream in = clientSocket.getInputStream()
    ) {
      assertEquals(REQUEST_TIMEOUT.toString(), readServerResponse(in).getStartLine().split(" ", 2)[1]);
    }
  }
}
