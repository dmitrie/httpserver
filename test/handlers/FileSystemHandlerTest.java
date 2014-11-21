package handlers;

import core.Request;
import core.Response;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileSystemHandlerTest {

  private FileSystemHandler fileSystemHandler;

  @Before
  public void setUp() throws Exception {
    String resource = FileSystemHandlerTest.class.getResource("/web").getPath();
    fileSystemHandler = new FileSystemHandler(resource);
  }

  @Test
  public void testHandleFileFound() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/test.html");
    Response response = new Response(request);

    fileSystemHandler.handle(request, response);

    assertEquals(OK, response.responseStatusCode);
    assertEquals("<h1>Example</h1>", response.getBody());
  }

  @Test
  public void testHandleFileNotFound() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/foo/bar/foo.html");
    Response response = new Response(request);

    fileSystemHandler.handle(request, response);

    assertEquals(NOT_FOUND, response.responseStatusCode);
  }

  @Test
  public void testHandleBadRequest() throws Exception {
    Request request = new Request();
    request.responseStatusCode = BAD_REQUEST;
    Response response = new Response(request);

    fileSystemHandler.handle(request, response);

    assertEquals(BAD_REQUEST, response.responseStatusCode);
    assertEquals(BAD_REQUEST.toString(), response.getBody());
  }

  @Test
  public void testHandleSpaceInFileName() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/folder/test%20file%201.html");
    Response response = new Response(request);

    fileSystemHandler.handle(request, response);

    assertEquals(OK, response.responseStatusCode);
    assertEquals("<h2>Example 1 in folder</h2>", response.getBody());
  }

  @Test
  public void testHandleNonASCIIFileInISO_8859_1_Returned() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/folder/inner%20folder/non-ASCII-test_in_ISO-8859-1.html");
    Response response = new Response(request);

    fileSystemHandler.handle(request, response);

    assertEquals(OK, response.responseStatusCode);
    assertEquals("41", response.headers.get("Content-length"));
    assertEquals("<h3>«Test» file inside inner folder</h3>\n", response.getBody());
  }

  @Test
  public void testGenerateDirectoryListingForFolder() throws Exception {
    List<String> links = fileSystemHandler.generateDirectoryListing(new URI("http://localhost:8080/folder/"));

    assertEquals(4, links.size());
    assertTrue(links.contains("<a href=\"/\">..</a>"));
    assertTrue(links.contains("<a href=\"/folder/test%20file%201.html\">test file 1.html</a>"));
    assertTrue(links.contains("<a href=\"/folder/test%20file%202.html\">test file 2.html</a>"));
    assertTrue(links.contains("<a href=\"/folder/inner%20folder/\">inner folder</a>"));
  }

  @Test
  public void testGenerateDirectoryListingForNestedFolder() throws Exception {
    List<String> links = fileSystemHandler.generateDirectoryListing(new URI("http://localhost:8080/folder/inner%20folder/"));

    assertEquals(2, links.size());
    assertTrue(links.contains("<a href=\"/folder/\">..</a>"));
    assertTrue(links.contains("<a href=\"/folder/inner%20folder/non-ASCII-test_in_ISO-8859-1.html\">non-ASCII-test_in_ISO-8859-1.html</a>"));
  }

  @Test
  public void testGenerateDirectoryListingForBasePath() throws Exception {
    List<String> links = fileSystemHandler.generateDirectoryListing(new URI("http://localhost:8080/"));

    assertEquals(2, links.size());
    assertTrue(links.contains("<a href=\"/folder/\">folder</a>"));
    assertTrue(links.contains("<a href=\"/test.html\">test.html</a>"));
  }
}