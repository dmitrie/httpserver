package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.IOException;

import static util.Helper.*;
import static util.HttpStatusCode.NOT_FOUND;
import static util.HttpStatusCode.OK;

public class FileSystemHandler extends Handler {

  private String documentRootPath;

  public FileSystemHandler(String documentRootPath) {
    this.documentRootPath = documentRootPath;
  }

  @Override
  public void handle(Request request, Response response) {
    if (response.getResponseStatusCode() != null)
      return;

    String localPath = combinePaths(getDocumentRootPath(), request.getRequestURI().getPath());

    switch (request.getMethod()) {
      case "GET": case "HEAD":
        try {
          response.setBody(readFile(localPath, response.getBodyCharset()));
          response.setResponseStatusCode(OK);
        } catch (IOException e) {
          response.setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
          response.setResponseStatusCode(NOT_FOUND);
        }
        response.setHeader("Content-Type", "text/html; charset=" + response.getBodyCharset());
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
