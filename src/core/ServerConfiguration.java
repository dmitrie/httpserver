package core;

import java.util.ArrayList;
import java.util.List;

public class ServerConfiguration {
  private int maximumURILength = 8190;
  private int requestTimeOut = 5000;
  private int portNumber = 8080;
  private String documentRootPath = "/home/kool/IdeaProjects/httpserver/test/web/";
  private List<String> implementedMethods = new ArrayList<String>(){{
    add("GET");
    add("POST");
    add("HEAD");
  }};

  public int getMaximumURILength() {
    return maximumURILength;
  }

  public int getRequestTimeOut() {
    return requestTimeOut;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public String getDocumentRootPath() {
    return documentRootPath;
  }

  public List<String> getImplementedMethods() {
    return implementedMethods;
  }

  public void setMaximumURILength(int maximumURILength) {
    this.maximumURILength = maximumURILength;
  }

  public void setRequestTimeOut(int requestTimeOut) {
    this.requestTimeOut = requestTimeOut;
  }

  public void setPortNumber(int portNumber) {
    this.portNumber = portNumber;
  }

  public void setDocumentRootPath(String documentRootPath) {
    this.documentRootPath = documentRootPath;
  }

  public void setImplementedMethods(List<String> implementedMethods) {
    this.implementedMethods = implementedMethods;
  }
}
