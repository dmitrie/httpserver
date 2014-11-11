package core;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static core.HttpStatusCode.*;
import static core.Server.*;

public class RequestHandler extends Thread {
  private final Socket clientSocket;
  private ServerConfiguration serverConfiguration;

  public RequestHandler(Socket clientSocket, ServerConfiguration serverConfiguration) {
    this.clientSocket = clientSocket;
    this.serverConfiguration = serverConfiguration;
  }

  @Override
  public void run() {
    try (
      Socket clientSocket = this.clientSocket;
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream()
    ) {
      clientSocket.setSoTimeout(serverConfiguration.getRequestTimeOut());

      Request request = new Request(serverConfiguration);
      try {
        request = new Request(in, serverConfiguration);
      } catch (SocketTimeoutException e) {
        respondWithError(out, request, REQUEST_TIMEOUT);
      } catch (Exception e) {
        respondWithError(out, request, INTERNAL_SERVER_ERROR);
      }

      try {
        Response response = htmlFileHandler(request);

        out.write(response.generateMessage());
        out.flush();
      } catch (Exception e) {
        respondWithError(out, request, INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      System.out.println("Exception caught when listening for a connection");
      System.out.println(e.getMessage());
    }
  }

  public Response htmlFileHandler(Request request) {
    Response response = new Response(request);
    if (response.getResponseStatusCode() != null)
      return response;

    try {
      response.setBody(readFile(combinePaths(serverConfiguration.getDocumentRootPath(), request.getRequestURI().getPath()), StandardCharsets.UTF_8));
      response.setResponseStatusCode(OK);
    } catch (IOException e) {
      response.setErrorBodyAndHeaders(NOT_FOUND);
      return response;
    }

    response.setHeader("Content-Type", "text/html; charset=" + response.getBodyEncoding());
    response.setHeader("Last-modified", getServerTime());
    return response;
  }
}
