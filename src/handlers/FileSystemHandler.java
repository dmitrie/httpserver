package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static core.Server.getServerTime;

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
          response.setErrorBodyAndHeaders(NOT_FOUND);
          return;
        }
        response.setHeader("Content-Type", "text/html; charset=" + response.getBodyCharset());
        response.setHeader("Last-modified", getServerTime());
        break;
      default:
        break;
    }
  }

  public static String readFile(String path, Charset charset) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, charset);
  }

  public static String combinePaths(String path1, String path2) {
    File file1 = new File(path1);
    File file2 = new File(file1, path2);
    return file2.getPath();
  }

  public String getDocumentRootPath() {
    return documentRootPath;
  }

  public void setDocumentRootPath(String documentRootPath) {
    this.documentRootPath = documentRootPath;
  }
}
