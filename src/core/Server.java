package core;

import handlers.FileSystemHandler;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Server {
  private volatile ServerConfiguration serverConfiguration;
  private volatile boolean serverIsRunning = false;
  private ServerSocket serverSocket;
  public Map<Pattern, Class<? extends Handler>> handlers = new LinkedHashMap<>();

  public Server(ServerConfiguration serverConfiguration) throws IOException {
    this.serverConfiguration = serverConfiguration;
    this.serverSocket = new ServerSocket(serverConfiguration.getPortNumber());
  }

  public Server() throws IOException {
    this.serverConfiguration = new ServerConfiguration();
    this.serverSocket = new ServerSocket(serverConfiguration.getPortNumber());
  }

  public static void main(String[] args) throws IOException {
    Server server = new Server();
    server.setHandler(".*", FileSystemHandler.class);
    server.start();
  }

  public void start() {
    serverIsRunning = true;
    ExecutorService executor = Executors.newFixedThreadPool(serverConfiguration.getNumberOfThreads());

    while(serverIsRunning) {
      try {
        Runnable worker = new RequestHandler(serverSocket.accept(), serverConfiguration, handlers);
        executor.execute(worker);
      } catch (Exception e) {
        System.out.println("Exception caught when listening for a connection");
        System.out.println(e.getMessage());
      }
    }

    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      System.out.println("Couldn't stop all threads");
      System.out.println(e.getMessage());
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

  public Map<Pattern, Class<? extends Handler>> getHandlers() {
    return handlers;
  }

  public void setHandlers(Map<Pattern, Class<? extends Handler>> handler) {
    this.handlers = handler;
  }

  public void setHandler(String pattern, Class<? extends Handler> handler) {
    this.handlers.put(Pattern.compile(pattern), handler);
  }

  public void stop() throws IOException {
    serverSocket.close();
    serverIsRunning = false;
  }
}