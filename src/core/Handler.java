package core;

public abstract class Handler {
  protected Request request;
  protected Response response;

  protected Handler(Request request, Response response) {
    this.request = request;
    this.response = response;
  }

  protected abstract void handle();
}
