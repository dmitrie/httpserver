package core;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static core.HttpStatusCode.*;

public class Server {
  private int portNumber;
  private String documentRootPath;
  private ServerSocket serverSocket;
  private boolean serverIsRunning = false;

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: java Server <port number> <documentRootPath>");
      System.exit(1);
    }

    int portNumber = Integer.parseInt(args[0]);
    String path = args[1].trim();

    Server server = new Server(portNumber, path);
    server.start();
  }

  public Server(int portNumber, String documentRootPath) throws IOException {
    this.portNumber = portNumber;
    this.documentRootPath = documentRootPath;
    this.serverSocket = new ServerSocket(portNumber);
  }

  public void start() {
    serverIsRunning = true;
    while(serverIsRunning) {
      handleRequest();
    }
  }

  public void handleRequest() {
    try (
      Socket clientSocket = serverSocket.accept();
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      InputStream in = clientSocket.getInputStream();
    ) {
      clientSocket.setSoTimeout(100);

      Request request = new Request();
      try {
        request = new Request(in);
      } catch (SocketTimeoutException e) {
        respondWithError(out, request, REQUEST_TIMEOUT);
      } catch (Exception e) {
        respondWithError(out, request, INTERNAL_SERVER_ERROR);
      }

      try {
        Response response = simpleHTMLHandler(request);

        out.write(response.toString());
        out.flush();
      } catch (Exception e) {
        respondWithError(out, request, INTERNAL_SERVER_ERROR);
      }
    }
    catch (Exception e) {
      System.out.println("Exception caught when listening for a connection");
      System.out.println(e.getMessage());
    }
  }

  public void respondWithError(PrintWriter out, Request request, HttpStatusCode code) throws UnsupportedEncodingException {
    Response response = new Response(request);
    response.setErrorBodyAndHeaders(code);
    out.write(response.toString());
    out.flush();
  }

  public Response simpleHTMLHandler(Request request) throws UnsupportedEncodingException {
    Response response = new Response(request);
    if (response.getStatusCode() != null)
      return response;

    try {
      response.setBody(readFile(combinePaths(documentRootPath, request.getPath()), StandardCharsets.UTF_8));
      response.setStatusCode(OK);
    } catch (IOException e) {
      response.setErrorBodyAndHeaders(NOT_FOUND);
      return response;
    }

    response.setHeader("Content-Type", "text/html; charset=" + response.getEncoding());
    response.setHeader("Last-modified", getServerTime());
    return response;
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

  public ServerSocket getServerSocket() {
    return serverSocket;
  }

  public void setServerSocket(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
  }

  public void stop() throws IOException {
    serverSocket.close();
    serverIsRunning = false;
  }
}