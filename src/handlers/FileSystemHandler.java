package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static util.Helper.combinePaths;
import static util.Helper.getServerTime;

public class FileSystemHandler extends Handler {

  private String documentRoot;

  public FileSystemHandler(String documentRoot) {
    this.documentRoot = documentRoot;
  }

  @Override
  public void handle(Request request, Response response) {
    if (response.responseStatusCode != null)
      return;

    String localPath = combinePaths(getDocumentRoot(), request.requestURI.getPath());

    switch (request.requestMethod) {
      case "GET":
      case "HEAD":
        try {
          String body = new String(Files.readAllBytes(Paths.get(localPath)), response.bodyCharset);
          response.setBody(body);
          response.responseStatusCode = OK;
        } catch (IOException e) {
          response.setBody("<div style=\"text-align: center;\"><h1 style=\"color: red;\">404 Error</h1><br>File not found</div>");
          response.responseStatusCode = NOT_FOUND;
        }
        response.setHeader("Content-Type", "text/html; charset=" + response.bodyCharset);
        response.setHeader("Last-modified", getServerTime());

        if ("HEAD".equals(request.requestMethod)) {
          response.setHeader("Content-Length", "" + response.calculateContentLength());
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
