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
  ServerConfiguration serverConfiguration;

  @Before
  public void setUp() throws Exception {
    boolean exceptionCaught;
    int numberOfAttempts = 5;
    do {
      try {
        serverConfiguration = new ServerConfiguration();
        serverConfiguration.setPortNumber(8361);
        serverConfiguration.setDocumentRootPath("/home/kool/IdeaProjects/httpserver/test/web/");
        serverConfiguration.setRequestTimeOut(500);
        server = new Server(serverConfiguration);
        serverThread = new Thread(() -> {
          server.start();
        });
        serverThread.start();
        exceptionCaught = false;
      } catch (Exception e) {
        exceptionCaught = true;
        Thread.sleep(500);
      }
    } while (exceptionCaught && numberOfAttempts-- > 0);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
    serverThread.interrupt();
  }

  @Test
  public void testHtmlFileHandlerSuccessFileReturned() throws Exception {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream();
    ) {
      out.write("GET /test.html HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();

      IncomingHttpMessage serverResponse = readServerResponse(in);
      String expectedBody = Server.readFile(Server.combinePaths(serverConfiguration.getDocumentRootPath(), "test.html"), new Response(new Request(server.getServerConfiguration())).getBodyEncoding());

      assertEquals("HTTP/1.1 " + OK, serverResponse.getStartLine());
      assertEquals("" + expectedBody.getBytes(serverResponse.getBodyEncoding()).length, serverResponse.getHeader("Content-length"));
      assertEquals(expectedBody, serverResponse.getBody());
    }
  }

  @Test
  public void testHtmlFileHandlerFileNotFound() throws Exception {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream();
    ) {
      out.write("GET /foo/bar/test.html HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();

      assertEquals("HTTP/1.1 " + NOT_FOUND, readServerResponse(in).getStartLine());
    }
  }

  @Test
  public void testHtmlFileHandlerBadRequest() throws Exception {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream();
    ) {
      out.write("foo bar\r\n\r\n");
      out.flush();

      assertEquals("HTTP/1.1 " + BAD_REQUEST, readServerResponse(in).getStartLine());
    }
  }

  @Test
  public void testRespondWithErrorRequestTimeOut() throws Exception {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream();
    ) {
      assertEquals(REQUEST_TIMEOUT.toString(), readServerResponse(in).getStartLine().split(" ", 2)[1]);
    }
  }

  public IncomingHttpMessage readServerResponse(InputStream in) throws IOException {
    IncomingHttpMessage serverResponse = new IncomingHttpMessage();
    String[] statusLineAndHeaders = serverResponse.readStartLineAndHeaders(in).split(CRLF, 2);

    serverResponse.setStartLine(statusLineAndHeaders[0]);
    serverResponse.parseAndSetHeaders(statusLineAndHeaders[1]);

    serverResponse.setBody(serverResponse.readBody(in));

    return serverResponse;
  }
}
