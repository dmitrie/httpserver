package core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import static core.HttpStatusCode.*;

public class RequestProcessor implements Runnable {
  private final Socket clientSocket;
  private Configuration configuration;
  private Map<Pattern, Handler> handlers;

  public RequestProcessor(Socket clientSocket, Configuration configuration, Map<Pattern, Handler> handlers) {
    this.clientSocket = clientSocket;
    this.configuration = configuration;
    this.handlers = handlers;
  }

  @Override
  public void run() {
    try (OutputStream out = clientSocket.getOutputStream()) {
      try (InputStream in = clientSocket.getInputStream()) {
        process(out, in);
      } finally {
        clientSocket.close();
      }
    } catch (Exception e) {
      System.out.println("Exception caught:");
      System.out.println(e.getMessage());
    }
  }

  private void process(OutputStream out, InputStream in) throws IOException {
    Request request = new Request(configuration);
    try {
      request = new Request(in, configuration);
      Response response = new Response(request);

      executeHandlers(request, response);
      if (response.getResponseStatusCode() == null)
        response.setStandardResponse(NOT_FOUND);

      out.write(response.generateMessage().getBytes(StandardCharsets.ISO_8859_1));
      out.flush();
    } catch (SocketTimeoutException e) {
      respondWithError(out, request, REQUEST_TIMEOUT);
    } catch (Exception e) {
      respondWithError(out, request, INTERNAL_SERVER_ERROR);
    }
  }

  private void executeHandlers(Request request, Response response) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchMethodException {
    if (request.getRequestURI() != null)
      for (Map.Entry<Pattern, Handler> entry : handlers.entrySet())
        if (entry.getKey().matcher(request.getRequestURI().getPath()).matches())
          entry.getValue().handle(request, response);
  }

  public void respondWithError(OutputStream out, Request request, HttpStatusCode code) throws IOException {
    Response response = new Response(request);
    response.setStandardResponse(code);
    out.write(response.generateMessage().getBytes(StandardCharsets.ISO_8859_1));
    out.flush();
  }
}
