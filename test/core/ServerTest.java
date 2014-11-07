package core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import static core.HttpRequestRegEx.CRLF;
import static core.HttpStatusCode.*;
import static java.nio.charset.Charset.forName;
import static org.junit.Assert.assertEquals;

public class ServerTest {
  Thread serverThread;
  Server server;

  String documentRootPath = "/home/kool/IdeaProjects/httpserver/test/web/";
  Map<String, String> headers = new LinkedCaseInsensitiveMap();
  String statusLine;
  String body;

  @Before
  public void setUp() throws Exception {
    boolean exceptionCaught;
    int maximumNumberOfAttempts = 5;
    do {
      try {
        server = new Server(8361, documentRootPath);
        serverThread = new Thread(() -> {
          server.start();
        });
        serverThread.start();
        exceptionCaught = false;
      } catch (Exception e) {
        exceptionCaught = true;
        Thread.sleep(500);
      }
    } while (exceptionCaught && maximumNumberOfAttempts-- > 0);
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

      readServerResponse(in);

      String expectedBody = Server.readFile(Server.combinePaths(documentRootPath, "test.html"), forName(new Response(new Request()).getEncoding()));

      assertEquals("HTTP/1.1 " + OK, statusLine);
      assertEquals("" + expectedBody.getBytes().length, headers.get("Content-length"));
      assertEquals(expectedBody, body);
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

      readServerResponse(in);

      String expectedBody = Server.readFile(Server.combinePaths(documentRootPath, "test.html"), forName(new Response(new Request()).getEncoding()));

      assertEquals("HTTP/1.1 " + NOT_FOUND, statusLine);
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

      readServerResponse(in);

      String expectedBody = Server.readFile(Server.combinePaths(documentRootPath, "test.html"), forName(new Response(new Request()).getEncoding()));

      assertEquals("HTTP/1.1 " + BAD_REQUEST, statusLine);
    }
  }

  @Test
  public void testRespondWithErrorRequestTimeOut() throws Exception {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream();
    ) {
      readServerResponse(in);

      assertEquals(REQUEST_TIMEOUT.toString(), statusLine.split(" ", 2)[1]);
    }
  }

  public void readServerResponse(InputStream in) throws IOException {
    Request serverResponse = new Request();
    String[] statusLineAndHeaders = serverResponse.readRequestLineAndHeaders(in).split(CRLF, 2);
    statusLine = statusLineAndHeaders[0];

    if (statusLineAndHeaders.length == 2)
      for (String header : statusLineAndHeaders[1].split(CRLF))
        serverResponse.setHeader(header);

    headers = serverResponse.getHeaders();
    body = serverResponse.readBody(in);
  }
}
