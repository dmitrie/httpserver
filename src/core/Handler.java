package core;

public abstract class Handler {
  protected abstract void handle(Request request, Response response);
}
