package handlers;

import core.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static core.Server.*;

public class StandardHandlers {

  public static void htmlFileHandler(Response response) {
    if (response.getResponseStatusCode() != null)
      return;

    try {
      response.setBody(readFile(
        combinePaths(
          response.getRequest().getServerConfiguration().getDocumentRootPath(),
          response.getRequest().getRequestURI().getPath()
        ), StandardCharsets.UTF_8));
      response.setResponseStatusCode(OK);
    } catch (IOException e) {
      response.setErrorBodyAndHeaders(NOT_FOUND);
      return;
    }

    response.setHeader("Content-Type", "text/html; charset=" + response.getBodyEncoding());
    response.setHeader("Last-modified", getServerTime());
  }
}
