package core;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class Request extends HttpMessage {
  public URI requestURI;
  public Map<String, LinkedList<String>> parameters = new LinkedHashMap<>();
}
