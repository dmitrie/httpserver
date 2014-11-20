package handlers;

import core.Request;
import core.Response;
import org.junit.Test;

import java.net.URI;

import static core.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;

public class FileSystemHandlerTest {

  @Test
  public void testHandleFileFound() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/test.html");
    Response response = new Response(request);

    new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web").handle(request, response);

    assertEquals(OK, response.responseStatusCode);
    assertEquals("<h1>Example</h1>", response.getBody());
  }

  @Test
  public void testHandleFileNotFound() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/foo/bar/foo.html");
    Response response = new Response(request);

    new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web").handle(request, response);

    assertEquals(NOT_FOUND, response.responseStatusCode);
  }

  @Test
  public void testHandleBadRequest() throws Exception {
    Request request = new Request();
    request.responseStatusCode = BAD_REQUEST;
    Response response = new Response(request);

    new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web").handle(request, response);

    assertEquals(BAD_REQUEST, response.responseStatusCode);
    assertEquals(BAD_REQUEST.toString(), response.getBody());
  }

  @Test
  public void testHandleSpaceInFileName() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/folder/test%20file%201.html");
    Response response = new Response(request);

    new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web").handle(request, response);

    assertEquals(OK, response.responseStatusCode);
    assertEquals("<h2>Example 1 in folder</h2>", response.getBody());
  }

  @Test
  public void testHandleNonASCIIFileInISO_8859_1_Returned() throws Exception {
    Request request = new Request();
    request.requestMethod = "GET";
    request.requestURI = new URI("http://localhost/folder/inner%20folder/non-ASCII-test_in_ISO-8859-1.html");
    Response response = new Response(request);

    new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web").handle(request, response);

    assertEquals(OK, response.responseStatusCode);
    assertEquals("41", response.headers.get("Content-length"));
    assertEquals("<h3>«Test» file inside inner folder</h3>\n", response.getBody());
  }
}