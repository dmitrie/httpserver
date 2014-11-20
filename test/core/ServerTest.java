package core;

import handlers.FileSystemHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.LinkedCaseInsensitiveMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import static core.HttpMessageReader.readExactNumberOfBytes;
import static core.HttpMessageReader.readStartLineAndHeaders;
import static core.HttpRequestRegEx.CRLF;
import static core.HttpRequestRegEx.getParsedBodyCharset;
import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;

public class ServerTest {
  Thread serverThread;
  Server server;

  String statusLine;
  Map<String, String> headers;
  String body;

  @Before
  public void setUp() throws Exception {
    statusLine = null;
    headers = new LinkedCaseInsensitiveMap();
    body = null;

    LinkedHashMap<Pattern, Handler> handlers = new LinkedHashMap<>();
    handlers.put(Pattern.compile(".*"), new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web/"));

    startServer(handlers);
  }

  public void startServer(LinkedHashMap<Pattern, Handler> handlers) {
    server = new Server(getConfiguration());
    server.handlers = handlers;
    serverThread = new Thread(server::start);
    serverThread.start();
    awaitCondition(10000, server::isRunning);
    System.out.println("Server started");
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
    serverThread.interrupt();
    awaitCondition(10000, () -> !server.isRunning());
    server = null;
    serverThread = null;
  }

  private boolean awaitCondition(long milliseconds, BooleanSupplier condition) {
    long endTime = System.currentTimeMillis() + milliseconds;
    while(System.currentTimeMillis() < endTime) {
      if (condition.getAsBoolean())
        return true;
    }
    return false;
  }

  private Configuration getConfiguration() {
    Configuration configuration = new Configuration();
    configuration.setPort(8361);
    configuration.setRequestTimeOut(500);
    return configuration;
  }

  public void parseHeaders(String multipleHeaders) {
    if (multipleHeaders == null || "".equals(multipleHeaders.trim()))
      return;

    for (String headerLine : multipleHeaders.split(CRLF)) {
      String[] headerAndValue = headerLine.split(":", 2);
      String header = headerAndValue[0];
      String value = headerAndValue[1].trim();

      headers.put(header, value);
    }
  }

  public void sendRequest(String request) throws IOException {
    try (
      Socket clientSocket = new Socket("localhost", 8361);
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream()
    ) {
      if (request != null) {
        out.write(request);
        out.flush();
      }
      readServerResponse(in);
    }
  }

  private void readServerResponse(InputStream in) throws IOException {
    String[] statusLineAndHeaders = readStartLineAndHeaders(in).split(CRLF, 2);

    statusLine = statusLineAndHeaders[0];
    parseHeaders(statusLineAndHeaders[1]);

    String contentLength = headers.get("Content-Length");
    if (contentLength != null) {
      Charset bodyCharset = getParsedBodyCharset(headers.get("Content-Type"));
      if (bodyCharset == null)
        bodyCharset = new Response().bodyCharset;
      body = readExactNumberOfBytes(in,Integer.parseInt(contentLength), bodyCharset);
    }
  }

  @Test
  public void testHtmlFileHandlerSuccessFileReturned() throws Exception {
    sendRequest("GET /test.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

    assertEquals("HTTP/1.1 " + OK, statusLine);
    assertEquals("16", headers.get("Content-length"));
    assertEquals("<h1>Example</h1>", body);
  }

  @Test
  public void testHtmlFileHandlerSuccessFileWithSpaceInNameReturned() throws Exception {
    sendRequest("GET /folder/test%20file%201.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

    assertEquals("HTTP/1.1 " + OK, statusLine);
    assertEquals("28", headers.get("Content-length"));
    assertEquals("<h2>Example 1 in folder</h2>", body);
  }

  @Test
  public void testHtmlFileHandlerFileNotFound() throws Exception {
    sendRequest("GET /foo/bar/test.html HTTP/1.1\r\nHost: localhost\r\n\r\n");
    assertEquals("HTTP/1.1 " + NOT_FOUND, statusLine);
  }

  @Test
  public void testHtmlFileHandlerBadRequest() throws Exception {
    sendRequest("foo bar\r\n\r\n");
    assertEquals("HTTP/1.1 " + BAD_REQUEST, statusLine);
  }

  @Test
  public void testRespondWithErrorRequestTimeOut() throws Exception {
    sendRequest(null);
    assertEquals("HTTP/1.1 " + REQUEST_TIMEOUT, statusLine);
  }

  @Test
  public void testMultipleHandlers() throws Exception {
    Map<Pattern, Handler> originalServerHandlers = server.handlers;
    try {
      tearDown();

      LinkedHashMap<Pattern, Handler> handlers = new LinkedHashMap<>();
      handlers.put(Pattern.compile(".*"), new HandlerOK());
      handlers.put(Pattern.compile("/abc/.*"), new HandlerNotFound());
      startServer(handlers);

      sendRequest("GET /test.html HTTP/1.1\r\nHost: www.google.com\r\n\r\n");
      assertEquals("HTTP/1.1 " + OK, statusLine);
      assertEquals("foo", body);

      sendRequest("GET /abc/test.html HTTP/1.1\r\nHost: www.google.com\r\n\r\n");
      assertEquals("HTTP/1.1 " + NOT_FOUND, statusLine);
      assertEquals("foobar", body);
    } finally {
      server.handlers = originalServerHandlers;
    }
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

  @Test
  public void testHtmlFileHandlerSuccessNonASCIIFileInISO_8859_1_Returned() throws Exception {
    sendRequest("GET /folder/inner%20folder/non-ASCII-test_in_ISO-8859-1.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

    assertEquals("HTTP/1.1 " + OK, statusLine);
    assertEquals("41", headers.get("Content-length"));
    assertEquals("<h3>«Test» file inside inner folder</h3>\n", body);
  }
}
