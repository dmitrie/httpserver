package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static core.Server.*;

public class FileSystemHandler extends Handler {

  private String documentRootPath = "/home/kool/IdeaProjects/httpserver/test/web/";

  @Override
  public void handle(Request request, Response response) {
    if (response.getResponseStatusCode() != null)
      return;

    String localPath = combinePaths(getDocumentRootPath(), request.getRequestURI().getPath());

    switch (request.getMethod()) {
      case "GET": case "HEAD":
        try {
          response.setBody(readFile(localPath, StandardCharsets.UTF_8));
          response.setResponseStatusCode(OK);
        } catch (IOException e) {
          response.setErrorBodyAndHeaders(NOT_FOUND);
          return;
        }
        response.setHeader("Content-Type", "text/html; charset=" + response.getBodyEncoding());
        response.setHeader("Last-modified", getServerTime());
        break;
      default:
        break;
    }
  }

  public String getDocumentRootPath() {
    return documentRootPath;
  }

  public void setDocumentRootPath(String documentRootPath) {
    this.documentRootPath = documentRootPath;
  }
}
