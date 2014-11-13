package core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
  private boolean absoluteUriIsAllowed = false;
  private Charset defaultCharset = StandardCharsets.UTF_8;
  private int numberOfThreads = 10;

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

  public boolean isAbsoluteUriIsAllowed() {
    return absoluteUriIsAllowed;
  }

  public Charset getDefaultCharset() {
    return defaultCharset;
  }

  public int getNumberOfThreads() {
    return numberOfThreads;
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

  public void setAbsoluteUriIsAllowed(boolean absoluteUriIsAllowed) {
    this.absoluteUriIsAllowed = absoluteUriIsAllowed;
  }

  public void setDefaultCharset(Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
  }

  public void setNumberOfThreads(int numberOfThreads) {
    this.numberOfThreads = numberOfThreads;
  }
}
