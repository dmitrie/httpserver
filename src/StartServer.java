import java.io.IOException;

public class StartServer {
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: java Server <port number> <path>");
      System.exit(1);
    }

    int portNumber = Integer.parseInt(args[0]);
    String path = args[1].trim();

    Server server = new Server(portNumber, path);
    server.start();
  }
}
