package core;

public abstract class Handler {
  protected ServerConfiguration serverConfiguration;

  protected Handler(ServerConfiguration serverConfiguration) {
    this.serverConfiguration = serverConfiguration;
  }

  protected abstract void handle(Request request, Response response);
}
