package core;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
  private int maximumURILength = 8190;
  private int requestTimeOut = 5000;
  private int port = 8080;
  private List<String> implementedMethods = new ArrayList<String>(){{
    add("GET");
    add("POST");
    add("HEAD");
  }};
  private List<String> supportedHttpVersions = new ArrayList<String>(){{
    add("HTTP/1.0");
    add("HTTP/1.1");
  }};
  private int numberOfThreads = 10;

  public int getMaximumURILength() {
    return maximumURILength;
  }

  public int getRequestTimeOut() {
    return requestTimeOut;
  }

  public int getPort() {
    return port;
  }

  public List<String> getImplementedMethods() {
    return implementedMethods;
  }

  public int getNumberOfThreads() {
    return numberOfThreads;
  }

  public List<String> getSupportedHttpVersions() {
    return supportedHttpVersions;
  }

  public void setMaximumURILength(int maximumURILength) {
    this.maximumURILength = maximumURILength;
  }

  public void setRequestTimeOut(int requestTimeOut) {
    this.requestTimeOut = requestTimeOut;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setImplementedMethods(List<String> implementedMethods) {
    this.implementedMethods = implementedMethods;
  }

  public void setNumberOfThreads(int numberOfThreads) {
    this.numberOfThreads = numberOfThreads;
  }

  public void setSupportedHttpVersions(List<String> supportedHttpVersions) {
    this.supportedHttpVersions = supportedHttpVersions;
  }
}
