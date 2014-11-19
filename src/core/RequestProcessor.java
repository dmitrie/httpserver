package core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import static core.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static core.HttpStatusCode.NOT_FOUND;

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
    Request request = new Request();
    try {
      RequestParser parser = new RequestParser(configuration);
      request = parser.setFields(in);
      ResponseOld response = new ResponseOld(request);

      executeHandlers(request, response);
      if (response.getResponseStatusCode() == null)
        response.setStandardResponse(NOT_FOUND);

      out.write(response.generateMessage().getBytes(StandardCharsets.ISO_8859_1));
      out.flush();
    } catch (Exception e) {
      respondWithError(out, request, INTERNAL_SERVER_ERROR);
    }
  }

  private void executeHandlers(Request request, ResponseOld response) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchMethodException {
    if (request.requestURI != null)
      for (Map.Entry<Pattern, Handler> entry : handlers.entrySet())
        if (entry.getKey().matcher(request.requestURI.getPath()).matches())
          entry.getValue().handle(request, response);
  }

  public void respondWithError(OutputStream out, Request request, HttpStatusCode code) throws IOException {
    ResponseOld response = new ResponseOld(request);
    response.setStandardResponse(code);
    out.write(response.generateMessage().getBytes(StandardCharsets.ISO_8859_1));
    out.flush();
  }
}
