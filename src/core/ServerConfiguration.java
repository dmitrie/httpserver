package core;

public class ServerConfiguration {
  private int maximumURILength = 8190;
  private int requestTimeOut = 5000;
  private int portNumber = 8080;
  private String documentRootPath = "/home/kool/IdeaProjects/httpserver/test/web/";

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
}
