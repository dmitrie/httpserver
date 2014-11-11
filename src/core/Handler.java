package core;

@FunctionalInterface
public interface Handler {
  void run(Response response);
}
