package core;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Server {
  private ServerConfiguration serverConfiguration;
  private ServerSocket serverSocket;
  private boolean serverIsRunning = false;
  public Map<Pattern, Handler> handlers = new LinkedHashMap<>();

  public Server(ServerConfiguration serverConfiguration) throws IOException {
    this.serverConfiguration = serverConfiguration;
    this.serverSocket = new ServerSocket(serverConfiguration.getPortNumber());
  }

  public Server() throws IOException {
    this.serverConfiguration = new ServerConfiguration();
    this.serverSocket = new ServerSocket(serverConfiguration.getPortNumber());
  }

  public void start() {
    serverIsRunning = true;
    while(serverIsRunning) {
      try {
        new RequestHandler(serverSocket.accept(), serverConfiguration, handlers).start();
      } catch (Exception e) {
        System.out.println("Exception caught when listening for a connection");
        System.out.println(e.getMessage());
      }
    }
  }

  public static void respondWithError(PrintWriter out, Request request, HttpStatusCode code) {
    Response response = new Response(request);
    response.setErrorBodyAndHeaders(code);
    out.write(response.generateMessage());
    out.flush();
  }

  public static String getServerTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    return dateFormat.format(calendar.getTime());
  }

  public static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  public static String combinePaths(String path1, String path2) {
    File file1 = new File(path1);
    File file2 = new File(file1, path2);
    return file2.getPath();
  }

  public boolean isServerIsRunning() {
    return serverIsRunning;
  }

  public ServerConfiguration getServerConfiguration() {
    return serverConfiguration;
  }

  public Map<Pattern, Handler> getHandlers() {
    return handlers;
  }

  public void setHandlers(Map<Pattern, Handler> handlers) {
    this.handlers = handlers;
  }

  public void setHandler(String pattern, Handler handler) {
    this.handlers.put(Pattern.compile(pattern), handler);
  }

  public void stop() throws IOException {
    serverSocket.close();
    serverIsRunning = false;
  }
}