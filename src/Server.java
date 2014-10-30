import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Server {
  private int portNumber;
  private String path;
  private ServerSocket serverSocket;

  public Server(int portNumber, String path) throws IOException {
    this.portNumber = portNumber;
    this.path = path;
    this.serverSocket = new ServerSocket(portNumber);
  }

  public void start() {
    while(true) {
      try (
        Socket clientSocket = serverSocket.accept();
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
      ) {
        Request request = createRequest(in);
        System.out.println(request);

        try {
          Response response = createResponse(request);
          System.out.println(response);

          out.write(response.toString());
          out.flush();
        } catch (Exception e) {
          Response response = new Response(request.getProtocol());
          response.setError(500);
          out.write(response.toString());
          out.flush();
        }
      } catch (Exception e) {
        System.out.println("Exception caught when listening for a connection");
        System.out.println(e.getMessage());
      }
    }
  }

  private Response createResponse(Request request) throws UnsupportedEncodingException {
    Response response = new Response(request.getProtocol());
    try {
      response.setBody(readFile(combinePaths(path, request.getPath()), StandardCharsets.UTF_8));
      response.setResponseCode("200 OK");
    } catch (IOException e) {
      response.setError(404);
      return response;
    }

    response.setHeader("Content-Type", "text/html; charset=" + response.getEncoding());
    response.setHeader("Last-modified", getServerTime());
    response.setHeader("Content-Length", "" + response.getContentLength());
    return response;
  }

  private Request createRequest(BufferedReader in) throws IOException {
    Request request = new Request(in.readLine());
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
      if (inputLine.isEmpty())
        break;
      request.setHeader(inputLine);
    }
    return request;
  }

  static String getServerTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    return dateFormat.format(calendar.getTime());
  }

  static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  static String combinePaths(String path1, String path2) {
    File file1 = new File(path1);
    File file2 = new File(file1, path2);
    return file2.getPath();
  }

}
