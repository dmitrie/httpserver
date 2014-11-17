package core;

import handlers.FileSystemHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Server {
  private Configuration configuration;
  private boolean running = false;
  private ServerSocket serverSocket;
  Map<Pattern, Handler> handlers = new LinkedHashMap<>();
  private ExecutorService threadPool;

  public Server() {
    this(new Configuration());
  }

  public Server(Configuration configuration) {
    this.configuration = configuration;
  }

  public static void main(String[] args) throws IOException {
    Server server = new Server();
    String documentRoot = args[0];
    server.setHandler(".*", new FileSystemHandler(documentRoot));
    server.start();
  }

  public void setHandler(String pattern, Handler handler) {
    handlers.put(Pattern.compile(pattern), handler);
  }

  public void start() {
    int port = configuration.getPortNumber();
    System.out.println("Starting server on port: " + port);
    serverSocket = initServerSocket(port);
    threadPool = Executors.newFixedThreadPool(configuration.getNumberOfThreads());
    try {
      listen();
    } catch (Exception e) {
      e.printStackTrace();
      stop();
    }
  }

  public void stop() {
    safeClose(serverSocket);
    stopThreads();
    running = false;
  }

  public Boolean isRunning() {
    return running;
  }

  private void listen() {
    running = true;
    while(running) {
      Runnable requestProcessor = new RequestProcessor(accept(), configuration, handlers);
      threadPool.execute(requestProcessor);
    }
  }

  private Socket accept() {
    try {
      Socket socket = serverSocket.accept();
      socket.setSoTimeout(configuration.getRequestTimeOut());
      return socket;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ServerSocket initServerSocket(int portNumber) {
    try {
      return new ServerSocket(portNumber);
    } catch (IOException e) {
      throw new RuntimeException("Could not start server", e);
    }
  }

  private void safeClose(java.io.Closeable closable) {
    try {
      closable.close();
    } catch (Throwable ignored) {}
  }

  private void stopThreads() {
    try {
      threadPool.shutdown();
      if(!threadPool.awaitTermination(10, TimeUnit.SECONDS))
        System.out.println("Couldn't stop all threads");
    } catch (Throwable ignored) {}
  }
}