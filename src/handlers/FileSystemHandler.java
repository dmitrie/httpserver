package handlers;

import core.Handler;
import core.Request;
import core.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static core.HttpStatusCode.NOT_FOUND;
import static core.HttpStatusCode.OK;
import static core.Server.getServerTime;
import static core.Server.readFile;

public class FileSystemHandler extends Handler {

  public FileSystemHandler(Request request, Response response) {
    super(request, response);
  }

  @Override
  public void handle() {
    if (response.getResponseStatusCode() != null)
      return;

    switch (request.getMethod()) {
      case "GET": case "HEAD":
        try {
          response.setBody(readFile(
            request.getLocalPath(), StandardCharsets.UTF_8));
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
}
