package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.IOException;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static util.Helper.*;

public class FileSystemHandler extends Handler {

  private String documentRoot;

  public FileSystemHandler(String documentRoot) {
    this.documentRoot = documentRoot;
  }

  @Override
  public void handle(Request request, Response response) {
    if (response.getResponseStatusCode() != null)
      return;

    String localPath = combinePaths(getDocumentRoot(), request.getRequestURI().getPath());

    switch (request.getMethod()) {
      case "GET":
      case "HEAD":
        try {
          response.setBody(readFile(localPath, response.getBodyCharset()));
          response.setResponseStatusCode(OK);
        } catch (IOException e) {
          response.setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
          response.setResponseStatusCode(NOT_FOUND);
        }
        response.setHeader("Content-Type", "text/html; charset=" + response.getBodyCharset());
        response.setHeader("Last-modified", getServerTime());

        if ("HEAD".equals(request.getMethod())) {
          response.setHeader("Content-Length", response.getContentLength());
          response.setBody(null);
        }
        break;
      default:
        break;
    }
  }

  public String getDocumentRoot() {
    return documentRoot;
  }
}
