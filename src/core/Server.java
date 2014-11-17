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

  public Server() {
    this(new ServerConfiguration());
  }

  public Server(ServerConfiguration serverConfiguration) {
    this.serverConfiguration = serverConfiguration;
  }

  public static void main(String[] args) throws IOException {
    Server server = new Server();
    server.setHandler(".*", new FileSystemHandler(args[0]));
    server.start();
  }

  public void start() throws IOException {
    serverIsRunning = true;
    serverSocket = new ServerSocket(serverConfiguration.getPortNumber());
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

    stopExecutor(executor);
  }

  public void stopExecutor(ExecutorService executor) {
    executor.shutdown();
    try {
      if(!executor.awaitTermination(10, TimeUnit.SECONDS))
        System.out.println("Couldn't stop all threads");
    } catch (InterruptedException ignored) {}
  }

  public void stop() throws IOException {
    serverSocket.close();
    serverIsRunning = false;
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
}