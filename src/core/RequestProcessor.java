package core;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.regex.Pattern;

import static core.HttpStatusCode.*;
import static core.Server.respondWithError;

public class RequestProcessor implements Runnable {
  private final Socket clientSocket;
  private ServerConfiguration serverConfiguration;
  private Map<Pattern, Handler> handlers;

  public RequestProcessor(Socket clientSocket, ServerConfiguration serverConfiguration, Map<Pattern, Handler> handlers) {
    this.clientSocket = clientSocket;
    this.serverConfiguration = serverConfiguration;
    this.handlers = handlers;
  }

  @Override
  public void run() {
    try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
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

  private void process(PrintWriter out, InputStream in) throws SocketException {
    clientSocket.setSoTimeout(serverConfiguration.getRequestTimeOut());

    Request request = new Request(serverConfiguration);
    try {
      request = new Request(in, serverConfiguration);
      Response response = new Response(request);

      executeHandlers(request, response);

      out.write(response.generateMessage());
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

    if (response.getResponseStatusCode() == null)
      response.setErrorBodyAndHeaders(FORBIDDEN);
  }
}
