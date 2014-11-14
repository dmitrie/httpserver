package core;

import handlers.FileSystemHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Server {
  private ServerConfiguration serverConfiguration;
  private boolean serverIsRunning = false;
  private ServerSocket serverSocket;
  public Map<Pattern, Handler> handlers = new LinkedHashMap<>();

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
    server.setHandler(".*", new FileSystemHandler("/home/kool/IdeaProjects/httpserver/test/web/"));
    server.start();
  }

  public void start() {
    serverIsRunning = true;
    ExecutorService executor = Executors.newFixedThreadPool(serverConfiguration.getNumberOfThreads());

    while(serverIsRunning) {
      try {
        Runnable worker = new RequestProcessor(serverSocket.accept(), serverConfiguration, handlers);
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

  public boolean isServerIsRunning() {
    return serverIsRunning;
  }

  public ServerConfiguration getServerConfiguration() {
    return serverConfiguration;
  }

  public Map<Pattern, Handler> getHandlers() {
    return handlers;
  }

  public void setHandlers(Map<Pattern, Handler> handler) {
    this.handlers = handler;
  }

  public void setHandler(String pattern, Handler handler) {
    this.handlers.put(Pattern.compile(pattern), handler);
  }

  public void stop() throws IOException {
    serverSocket.close();
    serverIsRunning = false;
  }
}